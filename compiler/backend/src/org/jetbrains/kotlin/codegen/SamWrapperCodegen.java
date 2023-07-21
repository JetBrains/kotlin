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

import kotlin.text.StringsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.backend.common.CodegenUtil;
import org.jetbrains.kotlin.backend.common.SamType;
import org.jetbrains.kotlin.codegen.context.ClassContext;
import org.jetbrains.kotlin.codegen.context.CodegenContext;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper;
import org.jetbrains.kotlin.config.LanguageFeature;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorImpl;
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil;
import org.jetbrains.kotlin.incremental.components.NoLookupLocation;
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin;
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKt;
import org.jetbrains.kotlin.resolve.scopes.MemberScope;
import org.jetbrains.kotlin.storage.LockBasedStorageManager;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.util.OperatorNameConventions;
import org.jetbrains.org.objectweb.asm.Label;
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;
import org.jetbrains.org.objectweb.asm.commons.Method;

import java.util.Collections;
import java.util.Map;

import static org.jetbrains.kotlin.codegen.AsmUtil.NO_FLAG_PACKAGE_PRIVATE;
import static org.jetbrains.kotlin.codegen.AsmUtil.asmTypeByFqNameWithoutInnerClasses;
import static org.jetbrains.kotlin.codegen.DescriptorAsmUtil.genAreEqualCall;
import static org.jetbrains.kotlin.resolve.jvm.AsmTypes.*;
import static org.jetbrains.org.objectweb.asm.Opcodes.*;

public class SamWrapperCodegen {
    private static final String FUNCTION_FIELD_NAME = "function";
    private static final Method GET_FUNCTION_DELEGATE = new Method("getFunctionDelegate", FUNCTION, new Type[0]);

    private final GenerationState state;
    private final boolean isInsideInline;
    private final KotlinTypeMapper typeMapper;
    private final SamType samType;
    private final MemberCodegen<?> parentCodegen;
    private final CodegenContext<?> parentContext;
    private final int visibility;
    private final int classFlags;
    public static final String SAM_WRAPPER_SUFFIX = "$0";

    public SamWrapperCodegen(
            @NotNull GenerationState state,
            @NotNull SamType samType,
            @NotNull MemberCodegen<?> parentCodegen,
            @NotNull CodegenContext<?> parentContext,
            boolean isInsideInline
    ) {
        this.state = state;
        this.isInsideInline = isInsideInline;
        this.typeMapper = state.getTypeMapper();
        this.samType = samType;
        this.parentCodegen = parentCodegen;
        this.parentContext = parentContext;
        visibility = isInsideInline ? ACC_PUBLIC : NO_FLAG_PACKAGE_PRIVATE;
        int synth = state.getLanguageVersionSettings().supportsFeature(LanguageFeature.SamWrapperClassesAreSynthetic) ? ACC_SYNTHETIC : 0;
        classFlags = visibility | ACC_FINAL | ACC_SUPER | synth;
    }

    @NotNull
    public Type genWrapper(
            @NotNull KtFile file,
            @NotNull CallableMemberDescriptor contextDescriptor
    ) {
        // Name for generated class, in form of whatever$1
        FqName fqName = getWrapperName(file, contextDescriptor);
        Type asmType = asmTypeByFqNameWithoutInnerClasses(fqName);

        // e.g. (T, T) -> Int
        KotlinType functionType = samType.getKotlinFunctionType();

        boolean isKotlinFunInterface = !(samType.getClassDescriptor() instanceof JavaClassDescriptor);

        ClassDescriptorImpl classDescriptor = new ClassDescriptorImpl(
                samType.getClassDescriptor().getContainingDeclaration(),
                fqName.shortName(),
                Modality.FINAL,
                ClassKind.CLASS,
                Collections.singleton(samType.getType()),
                SourceElement.NO_SOURCE,
                /* isExternal = */ false,
                LockBasedStorageManager.NO_LOCKS
        );
        classDescriptor.initialize(MemberScope.Empty.INSTANCE, Collections.emptySet(), null);

        // e.g. compare(T, T)
        SimpleFunctionDescriptor erasedInterfaceFunction = samType.getOriginalAbstractMethod().copy(
                classDescriptor,
                Modality.FINAL,
                DescriptorVisibilities.PUBLIC,
                CallableMemberDescriptor.Kind.SYNTHESIZED,
                /*copyOverrides=*/ false
        );

        ClassBuilder cv = state.getFactory().newVisitor(JvmDeclarationOriginKt.OtherOrigin(erasedInterfaceFunction), asmType, file);
        Type samAsmType = typeMapper.mapType(samType.getType());
        String[] superInterfaces = isKotlinFunInterface
                                   ? new String[] {samAsmType.getInternalName(), FUNCTION_ADAPTER.getInternalName()}
                                   : new String[] {samAsmType.getInternalName()};
        cv.defineClass(
                file,
                state.getConfig().getClassFileVersion(),
                classFlags,
                asmType.getInternalName(),
                null,
                OBJECT_TYPE.getInternalName(),
                superInterfaces
        );
        cv.visitSource(file.getName(), null);

        WriteAnnotationUtilKt.writeSyntheticClassMetadata(cv, state, isInsideInline);

        generateInnerClassInformation(file, asmType, cv);

        // e.g. ASM type for Function2
        Type functionAsmType = typeMapper.mapType(functionType);

        cv.newField(JvmDeclarationOriginKt.OtherOrigin(erasedInterfaceFunction),
                    ACC_SYNTHETIC | ACC_PRIVATE | ACC_FINAL,
                    FUNCTION_FIELD_NAME,
                    functionAsmType.getDescriptor(),
                    null,
                    null);

        generateConstructor(asmType, functionAsmType, cv);

        ClassContext context = state.getRootContext().intoClass(classDescriptor, OwnerKind.IMPLEMENTATION, state);
        FunctionCodegen functionCodegen = new FunctionCodegen(context, cv, state, parentCodegen);
        generateMethod(asmType, functionAsmType, erasedInterfaceFunction, functionType, functionCodegen);

        if (isKotlinFunInterface) {
            generateGetFunctionDelegate(cv, asmType, functionAsmType);
            generateEquals(cv, asmType, functionAsmType, samAsmType);
            generateHashCode(cv, asmType, functionAsmType);

            generateDelegatesToDefaultImpl(asmType, classDescriptor, samType.getClassDescriptor(), functionCodegen, state);
        }

        cv.done(state.getConfig().getGenerateSmapCopyToAnnotation());

        return asmType;
    }

    private void generateInnerClassInformation(@NotNull KtFile file, Type asmType, ClassBuilder cv) {
        parentCodegen.addSyntheticAnonymousInnerClass(new SyntheticInnerClassInfo(asmType.getInternalName(), classFlags));
        CodegenContext<?> outerContext = MemberCodegen.getNonInlineOuterContext(parentContext);
        assert outerContext != null :
                "Outer context for SAM wrapper " + asmType.getInternalName() + " is null, parentContext:" + parentContext;
        Type outerClassType = MemberCodegen.computeOuterClass(state.getTypeMapper(), state.getJvmDefaultMode(), file, outerContext);
        assert outerClassType != null :
                "Outer class for SAM wrapper " + asmType.getInternalName() + " is null, parentContext:" + parentContext;
        Method enclosingMethod = MemberCodegen.computeEnclosingMethod(state.getTypeMapper(), outerContext);
        cv.visitOuterClass(
                outerClassType.getInternalName(),
                enclosingMethod == null ? null : enclosingMethod.getName(),
                enclosingMethod == null ? null : enclosingMethod.getDescriptor()
        );
        cv.visitInnerClass(asmType.getInternalName(), null, null, classFlags);
    }

    private void generateConstructor(Type ownerType, Type functionType, ClassBuilder cv) {
        MethodVisitor mv = cv.newMethod(JvmDeclarationOriginKt.OtherOrigin(samType.getClassDescriptor()),
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
            @NotNull Type ownerType,
            @NotNull Type functionType,
            @NotNull SimpleFunctionDescriptor erasedInterfaceFunction,
            @NotNull KotlinType functionKotlinType,
            @NotNull FunctionCodegen functionCodegen
    ) {
        FunctionDescriptor invokeFunction = functionKotlinType.getMemberScope().getContributedFunctions(
                OperatorNameConventions.INVOKE, NoLookupLocation.FROM_BACKEND
        ).iterator().next().getOriginal();
        StackValue functionField = StackValue.field(functionType, ownerType, FUNCTION_FIELD_NAME, false, StackValue.none());
        functionCodegen.genSamDelegate(erasedInterfaceFunction, invokeFunction, functionField);

        // generate sam bridges
        // TODO: erasedInterfaceFunction is actually not an interface function, but function in generated class
        SimpleFunctionDescriptor originalInterfaceErased = samType.getOriginalAbstractMethod();
        ClosureCodegen.generateBridgesForSAM(originalInterfaceErased, erasedInterfaceFunction, functionCodegen);
    }

    private static void generateEquals(
            @NotNull ClassBuilder cv, @NotNull Type asmType, @NotNull Type functionAsmType, @NotNull Type samAsmType
    ) {
        MethodVisitor mv = cv.newMethod(JvmDeclarationOrigin.NO_ORIGIN, ACC_PUBLIC, "equals", "(Ljava/lang/Object;)Z", null, null);
        InstructionAdapter iv = new InstructionAdapter(mv);

        Label notEqual = new Label();
        iv.load(1, OBJECT_TYPE);
        iv.instanceOf(samAsmType);
        iv.ifeq(notEqual);
        iv.load(1, OBJECT_TYPE);
        iv.instanceOf(FUNCTION_ADAPTER);
        iv.ifeq(notEqual);

        iv.load(0, OBJECT_TYPE);
        iv.getfield(asmType.getInternalName(), FUNCTION_FIELD_NAME, functionAsmType.getDescriptor());
        iv.load(1, OBJECT_TYPE);
        iv.checkcast(FUNCTION_ADAPTER);
        iv.invokeinterface(FUNCTION_ADAPTER.getInternalName(), GET_FUNCTION_DELEGATE.getName(), GET_FUNCTION_DELEGATE.getDescriptor());
        genAreEqualCall(iv);
        iv.ifeq(notEqual);

        iv.iconst(1);
        Label exit = new Label();
        iv.goTo(exit);

        iv.visitLabel(notEqual);
        iv.iconst(0);

        iv.visitLabel(exit);
        iv.areturn(Type.BOOLEAN_TYPE);
        FunctionCodegen.endVisit(iv, "equals of SAM wrapper");
    }

    private static void generateHashCode(@NotNull ClassBuilder cv, @NotNull Type asmType, @NotNull Type functionAsmType) {
        MethodVisitor mv = cv.newMethod(JvmDeclarationOrigin.NO_ORIGIN, ACC_PUBLIC, "hashCode", "()I", null, null);
        InstructionAdapter iv = new InstructionAdapter(mv);
        iv.load(0, OBJECT_TYPE);
        iv.getfield(asmType.getInternalName(), FUNCTION_FIELD_NAME, functionAsmType.getDescriptor());
        iv.invokevirtual(OBJECT_TYPE.getInternalName(), "hashCode", "()I", false);
        iv.areturn(Type.INT_TYPE);
        FunctionCodegen.endVisit(iv, "hashCode of SAM wrapper");
    }

    private static void generateGetFunctionDelegate(@NotNull ClassBuilder cv, @NotNull Type asmType, @NotNull Type functionAsmType) {
        MethodVisitor mv = cv.newMethod(
                JvmDeclarationOrigin.NO_ORIGIN, ACC_PUBLIC,
                GET_FUNCTION_DELEGATE.getName(), GET_FUNCTION_DELEGATE.getDescriptor(), null, null
        );
        InstructionAdapter iv = new InstructionAdapter(mv);
        iv.load(0, asmType);
        iv.getfield(asmType.getInternalName(), FUNCTION_FIELD_NAME, functionAsmType.getDescriptor());
        iv.areturn(OBJECT_TYPE);
        FunctionCodegen.endVisit(iv, "getFunctionDelegate of SAM wrapper");
    }

    public static void generateDelegatesToDefaultImpl(
            @NotNull Type asmType,
            @NotNull ClassDescriptor classDescriptor,
            @NotNull ClassDescriptor funInterface,
            @NotNull FunctionCodegen functionCodegen,
            @NotNull GenerationState state
    ) {
        JvmKotlinType receiverType = new JvmKotlinType(asmType, classDescriptor.getDefaultType());

        for (DeclarationDescriptor descriptor : DescriptorUtils.getAllDescriptors(funInterface.getDefaultType().getMemberScope())) {
            if (!(descriptor instanceof CallableMemberDescriptor)) continue;
            CallableMemberDescriptor member = (CallableMemberDescriptor) descriptor;
            if (member.getModality() == Modality.ABSTRACT ||
                DescriptorVisibilities.isPrivate(member.getVisibility()) ||
                member.getVisibility() == DescriptorVisibilities.INVISIBLE_FAKE ||
                DescriptorUtils.isMethodOfAny(member)) continue;

            for (Map.Entry<FunctionDescriptor, FunctionDescriptor> entry : CodegenUtil.INSTANCE.copyFunctions(
                    member, member, classDescriptor, Modality.OPEN, DescriptorVisibilities.PUBLIC, CallableMemberDescriptor.Kind.DECLARATION, false
            ).entrySet()) {
                ClassBodyCodegen.generateDelegationToDefaultImpl(entry.getKey(), entry.getValue(), receiverType, functionCodegen, state, false);
            }
        }
    }

    @NotNull
    private FqName getWrapperName(
            @NotNull KtFile containingFile,
            CallableMemberDescriptor contextDescriptor
    ) {
        boolean hasPackagePartClass = !CodegenUtil.getMemberDeclarationsToGenerate(containingFile).isEmpty();
        FqName filePartFqName = JvmFileClassUtil.getFileClassInfoNoResolve(containingFile).getFileClassFqName();

        FqName outermostOwner;
        if (hasPackagePartClass) {
            outermostOwner = filePartFqName;
        }
        else {
            ClassifierDescriptor outermostClassifier = getOutermostParentClass(contextDescriptor);
            if (outermostClassifier == null) throw new IllegalStateException("Can't find outermost parent class for " + contextDescriptor);
            String internalName = typeMapper.mapType(outermostClassifier).getInternalName();
            outermostOwner = filePartFqName.parent().child(Name.identifier(StringsKt.substringAfterLast(internalName, '/', internalName)));
        }

        String shortName = String.format(
                "%s$sam%s$%s" + SAM_WRAPPER_SUFFIX,
                outermostOwner.shortName().asString(),
                (isInsideInline ? "$i" : ""),
                DescriptorUtils.getFqNameSafe(samType.getClassDescriptor()).asString().replace('.', '_')
        );
        return outermostOwner.parent().child(Name.identifier(shortName));
    }

    private static ClassDescriptor getOutermostParentClass(CallableMemberDescriptor contextDescriptor) {
        ClassDescriptor parent = DescriptorUtils.getParentOfType(contextDescriptor, ClassDescriptor.class, true);
        ClassDescriptor next;
        do {
            next = DescriptorUtils.getParentOfType(parent, ClassDescriptor.class, true);
            if (next != null) parent = next;
        }
        while (next != null);
        return parent;
    }
}
