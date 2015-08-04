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
import org.jetbrains.kotlin.codegen.context.CodegenContext;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.codegen.state.JetTypeMapper;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorImpl;
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl;
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor;
import org.jetbrains.kotlin.load.kotlin.PackageClassUtils;
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.scopes.LookupLocation;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.types.expressions.OperatorConventions;
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;

import java.util.Collections;

import static org.jetbrains.kotlin.codegen.AsmUtil.*;
import static org.jetbrains.kotlin.load.java.JvmAnnotationNames.KotlinSyntheticClass;
import static org.jetbrains.kotlin.resolve.jvm.AsmTypes.OBJECT_TYPE;
import static org.jetbrains.kotlin.resolve.jvm.diagnostics.DiagnosticsPackage.OtherOrigin;
import static org.jetbrains.org.objectweb.asm.Opcodes.*;

public class SamWrapperCodegen {
    private static final String FUNCTION_FIELD_NAME = "function";

    private final GenerationState state;
    private final JetTypeMapper typeMapper;
    private final SamType samType;
    private final MemberCodegen<?> parentCodegen;

    public SamWrapperCodegen(@NotNull GenerationState state, @NotNull SamType samType, @NotNull MemberCodegen<?> parentCodegen) {
        this.state = state;
        this.typeMapper = state.getTypeMapper();
        this.samType = samType;
        this.parentCodegen = parentCodegen;
    }

    public Type genWrapper(@NotNull JetFile file) {
        // Name for generated class, in form of whatever$1
        FqName fqName = getWrapperName(file);
        Type asmType = asmTypeByFqNameWithoutInnerClasses(fqName);

        // e.g. (T, T) -> Int
        JetType functionType = samType.getKotlinFunctionType();

        ClassDescriptor classDescriptor = new ClassDescriptorImpl(
                samType.getJavaClassDescriptor().getContainingDeclaration(),
                fqName.shortName(),
                Modality.FINAL,
                Collections.singleton(samType.getType()),
                SourceElement.NO_SOURCE
        );
        // e.g. compare(T, T)
        SimpleFunctionDescriptor erasedInterfaceFunction = samType.getAbstractMethod().getOriginal().copy(
                classDescriptor,
                Modality.FINAL,
                Visibilities.PUBLIC,
                CallableMemberDescriptor.Kind.SYNTHESIZED,
                /*copyOverrides=*/ false
        );

        ClassBuilder cv = state.getFactory().newVisitor(OtherOrigin(erasedInterfaceFunction), asmType, file);
        cv.defineClass(file,
                       V1_6,
                       ACC_FINAL,
                       asmType.getInternalName(),
                       null,
                       OBJECT_TYPE.getInternalName(),
                       new String[]{ typeMapper.mapType(samType.getType()).getInternalName() }
        );
        cv.visitSource(file.getName(), null);

        writeKotlinSyntheticClassAnnotation(cv, KotlinSyntheticClass.Kind.SAM_WRAPPER);

        // e.g. ASM type for Function2
        Type functionAsmType = typeMapper.mapType(functionType);

        cv.newField(OtherOrigin(erasedInterfaceFunction),
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
        MethodVisitor mv = cv.newMethod(OtherOrigin(samType.getJavaClassDescriptor()),
                                        NO_FLAG_PACKAGE_PRIVATE, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, functionType), null, null);

        if (state.getClassBuilderMode() == ClassBuilderMode.FULL) {
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
            FunctionCodegen.endVisit(iv, "constructor of SAM wrapper", null);
        }
    }

    private void generateMethod(
            Type ownerType,
            Type functionType,
            ClassBuilder cv,
            SimpleFunctionDescriptor erasedInterfaceFunction,
            JetType functionJetType
    ) {
        // using static context to avoid creating ClassDescriptor and everything else
        FunctionCodegen codegen = new FunctionCodegen(CodegenContext.STATIC.intoClass(
                (ClassDescriptor) erasedInterfaceFunction.getContainingDeclaration(), OwnerKind.IMPLEMENTATION, state), cv, state, parentCodegen);

        FunctionDescriptor invokeFunction =
                functionJetType.getMemberScope().getFunctions(OperatorConventions.INVOKE, LookupLocation.NO_LOCATION).iterator().next().getOriginal();
        StackValue functionField = StackValue.field(functionType, ownerType, FUNCTION_FIELD_NAME, false, StackValue.none());
        codegen.genDelegate(erasedInterfaceFunction, invokeFunction, functionField);

        // generate sam bridges
        // TODO: erasedInterfaceFunction is actually not an interface function, but function in generated class
        SimpleFunctionDescriptor originalInterfaceErased = samType.getAbstractMethod().getOriginal();
        SimpleFunctionDescriptorImpl descriptorForBridges = SimpleFunctionDescriptorImpl
                .create(erasedInterfaceFunction.getContainingDeclaration(), erasedInterfaceFunction.getAnnotations(), originalInterfaceErased.getName(),
                        CallableMemberDescriptor.Kind.DECLARATION, erasedInterfaceFunction.getSource());

        descriptorForBridges
                .initialize(null, originalInterfaceErased.getDispatchReceiverParameter(), originalInterfaceErased.getTypeParameters(),
                            originalInterfaceErased.getValueParameters(), originalInterfaceErased.getReturnType(), Modality.OPEN,
                            originalInterfaceErased.getVisibility());

        descriptorForBridges.addOverriddenDescriptor(originalInterfaceErased);
        codegen.generateBridges(descriptorForBridges);
    }

    @NotNull
    private FqName getWrapperName(@NotNull JetFile containingFile) {
        FqName packageClassFqName = PackageClassUtils.getPackageClassFqName(containingFile.getPackageFqName());
        JavaClassDescriptor descriptor = samType.getJavaClassDescriptor();
        int hash = PackagePartClassUtils.getPathHashCode(containingFile.getVirtualFile()) * 31 +
                DescriptorUtils.getFqNameSafe(descriptor).hashCode();
        String shortName = String.format(
                "%s$sam$%s$%08x",
                packageClassFqName.shortName().asString(),
                descriptor.getName().asString(),
                hash
        );
        return packageClassFqName.parent().child(Name.identifier(shortName));
    }
}
