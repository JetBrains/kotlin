/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.codegen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorImpl;
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl;
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil;
import org.jetbrains.kotlin.incremental.components.NoLookupLocation;
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor;
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt;
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKt;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.util.OperatorNameConventions;
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;

import java.util.Collections;

import static org.jetbrains.kotlin.codegen.AsmUtil.NO_FLAG_PACKAGE_PRIVATE;
import static org.jetbrains.kotlin.codegen.AsmUtil.asmTypeByFqNameWithoutInnerClasses;
import static org.jetbrains.kotlin.resolve.jvm.AsmTypes.OBJECT_TYPE;
import static org.jetbrains.org.objectweb.asm.Opcodes.*;

public class SamWrapperCodegen {
    private static final String FUNCTION_FIELD_NAME = "function";

    private final GenerationState state;
    private final boolean isInsideInline;
    private final KotlinTypeMapper typeMapper;
    private final SamType samType;
    private final MemberCodegen<?> parentCodegen;
    private final int visibility;

    public SamWrapperCodegen(
            @NotNull GenerationState state,
            @NotNull SamType samType,
            @NotNull MemberCodegen<?> parentCodegen,
            boolean isInsideInline
    ) {
        this.state = state;
        this.isInsideInline = isInsideInline;
        this.typeMapper = state.getTypeMapper();
        this.samType = samType;
        this.parentCodegen = parentCodegen;
        visibility = isInsideInline ? ACC_PUBLIC : NO_FLAG_PACKAGE_PRIVATE;
    }

    @NotNull
    public Type genWrapper(@NotNull KtFile file) {
        // Name for generated class, in form of whatever$1
        FqName fqName = getWrapperName(file);
        Type asmType = asmTypeByFqNameWithoutInnerClasses(fqName);

        // e.g. (T, T) -> Int
        KotlinType functionType = samType.getKotlinFunctionType();

        ClassDescriptor classDescriptor = new ClassDescriptorImpl(
                samType.getJavaClassDescriptor().getContainingDeclaration(),
                fqName.shortName(),
                Modality.FINAL,
                ClassKind.CLASS,
                Collections.singleton(samType.getType()),
                SourceElement.NO_SOURCE,
                /* isExternal = */ false
        );
        // e.g. compare(T, T)
        SimpleFunctionDescriptor erasedInterfaceFunction = samType.getOriginalAbstractMethod().copy(
                classDescriptor,
                Modality.FINAL,
                Visibilities.PUBLIC,
                CallableMemberDescriptor.Kind.SYNTHESIZED,
                /*copyOverrides=*/ false
        );

        ClassBuilder cv = state.getFactory().newVisitor(JvmDeclarationOriginKt.OtherOrigin(erasedInterfaceFunction), asmType, file);
        cv.defineClass(file,
                       state.getClassFileVersion(),
                       ACC_FINAL | ACC_SUPER | visibility,
                       asmType.getInternalName(),
                       null,
                       OBJECT_TYPE.getInternalName(),
                       new String[]{ typeMapper.mapType(samType.getType()).getInternalName() }
        );
        cv.visitSource(file.getName(), null);

        WriteAnnotationUtilKt.writeSyntheticClassMetadata(cv, state);

        // e.g. ASM type for Function2
        Type functionAsmType = typeMapper.mapType(functionType);

        cv.newField(JvmDeclarationOriginKt.OtherOrigin(erasedInterfaceFunction),
                    ACC_SYNTHETIC | ACC_PRIVATE | ACC_FINAL,
                    FUNCTION_FIELD_NAME,
                    functionAsmType.getDescriptor(),
                    null,
                    null);

        generateConstructor(asmType, functionAsmType, cv);
        generateMethod(asmType, functionAsmType, cv, erasedInterfaceFunction, functionType);

        cv.done();

        return asmType;
    }

    private void generateConstructor(Type ownerType, Type functionType, ClassBuilder cv) {
        MethodVisitor mv = cv.newMethod(JvmDeclarationOriginKt.OtherOrigin(samType.getJavaClassDescriptor()),
                                        visibility, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, functionType), null, null);

        if (state.getClassBuilderMode().generateBodies) {
            mv.visitCode();
            InstructionAdapter iv = new InstructionAdapter(mv);

            // super constructor
            iv.load(0, OBJECT_TYPE);
            iv.invokespecial(OBJECT_TYPE.getInternalName(), "<init>", "()V", false);

            // save parameter to field
            iv.load(0, OBJECT_TYPE);
            iv.load(1, functionType);
            iv.putfield(ownerType.getInternalName(), FUNCTION_FIELD_NAME, functionType.getDescriptor());

            iv.visitInsn(RETURN);
            FunctionCodegen.endVisit(iv, "constructor of SAM wrapper");
        }
    }

    private void generateMethod(
            Type ownerType,
            Type functionType,
            ClassBuilder cv,
            SimpleFunctionDescriptor erasedInterfaceFunction,
            KotlinType functionJetType
    ) {
        // using root context to avoid creating ClassDescriptor and everything else
        FunctionCodegen codegen = new FunctionCodegen(state.getRootContext().intoClass(
                (ClassDescriptor) erasedInterfaceFunction.getContainingDeclaration(), OwnerKind.IMPLEMENTATION, state), cv, state, parentCodegen);

        FunctionDescriptor invokeFunction =
                functionJetType.getMemberScope().getContributedFunctions(OperatorNameConventions.INVOKE, NoLookupLocation.FROM_BACKEND).iterator().next().getOriginal();
        StackValue functionField = StackValue.field(functionType, ownerType, FUNCTION_FIELD_NAME, false, StackValue.none());
        codegen.genSamDelegate(erasedInterfaceFunction, invokeFunction, functionField);

        // generate sam bridges
        // TODO: erasedInterfaceFunction is actually not an interface function, but function in generated class
        SimpleFunctionDescriptor originalInterfaceErased = samType.getOriginalAbstractMethod();
        SimpleFunctionDescriptorImpl descriptorForBridges = SimpleFunctionDescriptorImpl
                .create(erasedInterfaceFunction.getContainingDeclaration(), erasedInterfaceFunction.getAnnotations(), originalInterfaceErased.getName(),
                        CallableMemberDescriptor.Kind.DECLARATION, erasedInterfaceFunction.getSource());

        descriptorForBridges
                .initialize(null, originalInterfaceErased.getDispatchReceiverParameter(), originalInterfaceErased.getTypeParameters(),
                            originalInterfaceErased.getValueParameters(), originalInterfaceErased.getReturnType(),
                            Modality.OPEN, originalInterfaceErased.getVisibility());

        DescriptorUtilsKt.setSingleOverridden(descriptorForBridges, originalInterfaceErased);
        codegen.generateBridges(descriptorForBridges);
    }

    @NotNull
    private FqName getWrapperName(@NotNull KtFile containingFile) {
        FqName fileClassFqName = JvmFileClassUtil.getFileClassInfoNoResolve(containingFile).getFileClassFqName();
        JavaClassDescriptor descriptor = samType.getJavaClassDescriptor();
        int hash = PackagePartClassUtils.getPathHashCode(containingFile.getVirtualFile()) * 31 +
                DescriptorUtils.getFqNameSafe(descriptor).hashCode();
        String shortName = String.format(
                "%s$sam$%s%s$%08x",
                fileClassFqName.shortName().asString(),
                descriptor.getName().asString(),
                (isInsideInline ? "$i" : ""),
                hash
        );
        return fileClassFqName.parent().child(Name.identifier(shortName));
    }
}
