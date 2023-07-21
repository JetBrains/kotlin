/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen;

import com.intellij.openapi.util.Pair;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.ArrayUtil;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.codegen.binding.CalculatedClosure;
import org.jetbrains.kotlin.codegen.binding.CodegenBinding;
import org.jetbrains.kotlin.codegen.context.CodegenContext;
import org.jetbrains.kotlin.codegen.intrinsics.HashCode;
import org.jetbrains.kotlin.codegen.intrinsics.IntrinsicMethods;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper;
import org.jetbrains.kotlin.config.JvmDefaultMode;
import org.jetbrains.kotlin.config.JvmTarget;
import org.jetbrains.kotlin.config.LanguageFeature;
import org.jetbrains.kotlin.config.LanguageVersionSettings;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.incremental.components.NoLookupLocation;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil;
import org.jetbrains.kotlin.metadata.jvm.serialization.JvmStringTable;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.protobuf.MessageLite;
import org.jetbrains.kotlin.renderer.DescriptorRenderer;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.InlineClassDescriptorResolver;
import org.jetbrains.kotlin.resolve.InlineClassesUtilsKt;
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver;
import org.jetbrains.kotlin.resolve.inline.InlineUtil;
import org.jetbrains.kotlin.resolve.jvm.AsmTypes;
import org.jetbrains.kotlin.resolve.jvm.InlineClassManglingRulesKt;
import org.jetbrains.kotlin.resolve.jvm.RuntimeAssertionInfo;
import org.jetbrains.kotlin.resolve.jvm.annotations.JvmAnnotationUtilKt;
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin;
import org.jetbrains.kotlin.serialization.DescriptorSerializer;
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.SimpleType;
import org.jetbrains.org.objectweb.asm.*;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;

import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.kotlin.codegen.AsmUtil.*;
import static org.jetbrains.kotlin.codegen.CodegenUtilKt.isToArrayFromCollection;
import static org.jetbrains.kotlin.codegen.JvmCodegenUtil.isConstOrHasJvmFieldAnnotation;
import static org.jetbrains.kotlin.codegen.JvmCodegenUtil.isJvmInterface;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.*;
import static org.jetbrains.kotlin.resolve.inline.InlineOnlyKt.isInlineOnlyPrivateInBytecode;
import static org.jetbrains.kotlin.resolve.inline.InlineOnlyKt.isInlineWithReified;
import static org.jetbrains.kotlin.resolve.jvm.AsmTypes.JAVA_STRING_TYPE;
import static org.jetbrains.kotlin.resolve.jvm.AsmTypes.OBJECT_TYPE;
import static org.jetbrains.kotlin.resolve.jvm.annotations.JvmAnnotationUtilKt.hasJvmSyntheticAnnotation;
import static org.jetbrains.kotlin.types.TypeUtils.isNullableType;
import static org.jetbrains.org.objectweb.asm.Opcodes.*;

public class DescriptorAsmUtil {
    private DescriptorAsmUtil() {
    }

    @NotNull
    public static String getNameForCapturedReceiverField(
            @NotNull CallableDescriptor descriptor,
            @NotNull BindingContext bindingContext,
            @NotNull LanguageVersionSettings languageVersionSettings
    ) {
        return getLabeledThisNameForReceiver(
                descriptor, bindingContext, languageVersionSettings, LABELED_THIS_FIELD, CAPTURED_RECEIVER_FIELD);
    }

    @NotNull
    public static String getNameForReceiverParameter(
            @NotNull CallableDescriptor descriptor,
            @NotNull BindingContext bindingContext,
            @NotNull LanguageVersionSettings languageVersionSettings
    ) {
        return getLabeledThisNameForReceiver(
                descriptor, bindingContext, languageVersionSettings, LABELED_THIS_PARAMETER, RECEIVER_PARAMETER_NAME);
    }

    @NotNull
    private static String getLabeledThisNameForReceiver(
            @NotNull CallableDescriptor descriptor,
            @NotNull BindingContext bindingContext,
            @NotNull LanguageVersionSettings languageVersionSettings,
            @NotNull String prefix,
            @NotNull String defaultName
    ) {
        if (!languageVersionSettings.supportsFeature(LanguageFeature.NewCapturedReceiverFieldNamingConvention)) {
            return defaultName;
        }

        Name callableName = null;

        if (descriptor instanceof FunctionDescriptor) {
            String labelName = bindingContext.get(CodegenBinding.CALL_LABEL_FOR_LAMBDA_ARGUMENT, (FunctionDescriptor) descriptor);
            if (labelName != null) {
                return getLabeledThisName(labelName, prefix, defaultName);
            }

            if (descriptor instanceof VariableAccessorDescriptor) {
                VariableAccessorDescriptor accessor = (VariableAccessorDescriptor) descriptor;
                callableName = accessor.getCorrespondingVariable().getName();
            }
        }

        if (callableName == null) {
            callableName = descriptor.getName();
        }

        if (callableName.isSpecial()) {
            return defaultName;
        }

        return getLabeledThisName(callableName.asString(), prefix, defaultName);
    }

    @NotNull
    public static Type boxType(@NotNull Type type, @NotNull KotlinType kotlinType, @NotNull KotlinTypeMapper typeMapper) {
        if (InlineClassesUtilsKt.isInlineClassType(kotlinType)) {
            return typeMapper.mapTypeAsDeclaration(kotlinType);
        }

        Type boxedPrimitiveType = boxPrimitiveType(type);
        return boxedPrimitiveType != null ? boxedPrimitiveType : type;
    }

    public static boolean isAbstractMethod(FunctionDescriptor functionDescriptor, OwnerKind kind, JvmDefaultMode jvmDefaultMode) {
        return (functionDescriptor.getModality() == Modality.ABSTRACT ||
                (isJvmInterface(functionDescriptor.getContainingDeclaration()) && !JvmAnnotationUtilKt
                        .isCompiledToJvmDefault(functionDescriptor, jvmDefaultMode)))
               && !isStaticMethod(kind, functionDescriptor);
    }

    public static boolean isStaticMethod(OwnerKind kind, CallableMemberDescriptor functionDescriptor) {
        return isStaticKind(kind) ||
               KotlinTypeMapper.isStaticAccessor(functionDescriptor) ||
               CodegenUtilKt.isJvmStaticInObjectOrClassOrInterface(functionDescriptor);
    }

    public static boolean isStaticKind(OwnerKind kind) {
        return kind == OwnerKind.PACKAGE || kind == OwnerKind.DEFAULT_IMPLS || kind == OwnerKind.ERASED_INLINE_CLASS;
    }

    public static int getMethodAsmFlags(FunctionDescriptor functionDescriptor, OwnerKind kind, GenerationState state) {
        return getMethodAsmFlags(functionDescriptor, kind, state.getDeprecationProvider(), state.getJvmDefaultMode());
    }

    public static int getMethodAsmFlags(
            FunctionDescriptor functionDescriptor,
            OwnerKind kind,
            DeprecationResolver deprecationResolver,
            JvmDefaultMode jvmDefaultMode
    ) {
        int flags = getCommonCallableFlags(functionDescriptor, kind, deprecationResolver);

        for (AnnotationCodegen.JvmFlagAnnotation flagAnnotation : AnnotationCodegen.METHOD_FLAGS) {
            flags |= flagAnnotation.getJvmFlag(functionDescriptor.getOriginal());
        }

        if (functionDescriptor.getOriginal().isExternal()) {
            flags |= Opcodes.ACC_NATIVE;
        }

        if (CodegenUtilKt.isJvmStaticInCompanionObject(functionDescriptor)) {
            // Native method will be a member of the class, the companion object method will be delegated to it
            flags &= ~Opcodes.ACC_NATIVE;
        }

        if (functionDescriptor.getModality() == Modality.FINAL && !(functionDescriptor instanceof ConstructorDescriptor)) {
            DeclarationDescriptor containingDeclaration = functionDescriptor.getContainingDeclaration();
            if (!isJvmInterface(containingDeclaration)) {
                flags |= ACC_FINAL;
            }
        }

        if (isStaticMethod(kind, functionDescriptor)) {
            flags |= ACC_STATIC;
        }

        if (isAbstractMethod(functionDescriptor, kind, jvmDefaultMode)) {
            flags |= ACC_ABSTRACT;
        }

        if (KotlinTypeMapper.isAccessor(functionDescriptor) ||
            hasJvmSyntheticAnnotation(functionDescriptor) ||
            isInlineClassWrapperConstructor(functionDescriptor, kind) ||
            InlineClassDescriptorResolver.isSynthesizedBoxMethod(functionDescriptor) ||
            InlineClassDescriptorResolver.isSynthesizedUnboxMethod(functionDescriptor)
        ) {
            flags |= ACC_SYNTHETIC;
        }

        return flags;
    }

    private static boolean isInlineClassWrapperConstructor(@NotNull FunctionDescriptor functionDescriptor, @Nullable OwnerKind kind) {
        if (!(functionDescriptor instanceof ConstructorDescriptor)) return false;
        ClassDescriptor classDescriptor = ((ConstructorDescriptor) functionDescriptor).getConstructedClass();
        return InlineClassesUtilsKt.isInlineClass(classDescriptor) && kind == OwnerKind.IMPLEMENTATION;
    }

    public static int getCommonCallableFlags(FunctionDescriptor functionDescriptor, @NotNull GenerationState state) {
        return getCommonCallableFlags(functionDescriptor, null, state.getDeprecationProvider());
    }

    private static int getCommonCallableFlags(
            FunctionDescriptor functionDescriptor,
            @Nullable OwnerKind kind,
            @NotNull DeprecationResolver deprecationResolver
    ) {
        int flags = getVisibilityAccessFlag(functionDescriptor, kind);
        flags |= getVarargsFlag(functionDescriptor);
        flags |= getDeprecatedAccessFlag(functionDescriptor);
        if (deprecationResolver.isDeprecatedHidden(functionDescriptor) || isInlineWithReified(functionDescriptor)) {
            flags |= ACC_SYNTHETIC;
        }
        return flags;
    }

    public static int getVisibilityAccessFlag(@NotNull MemberDescriptor descriptor) {
        return getVisibilityAccessFlag(descriptor, null);
    }

    private static int getVisibilityAccessFlag(@NotNull MemberDescriptor descriptor, @Nullable OwnerKind kind) {
        Integer specialCase = specialCaseVisibility(descriptor, kind);
        if (specialCase != null) {
            return specialCase;
        }
        DescriptorVisibility visibility = descriptor.getVisibility();
        Integer defaultMapping = getVisibilityAccessFlag(visibility);
        if (defaultMapping == null) {
            throw new IllegalStateException(visibility + " is not a valid visibility in backend for " + DescriptorRenderer.DEBUG_TEXT.render(descriptor));
        }
        return defaultMapping;
    }

    @Nullable
    public static Integer getVisibilityAccessFlag(DescriptorVisibility visibility) {
        return AsmUtil.getVisibilityAccessFlag(visibility.getDelegate());
    }

    /*
        Use this method to get visibility flag for class to define it in byte code (v.defineClass method).
        For other cases use getVisibilityAccessFlag(MemberDescriptor descriptor)
        Classes in byte code should be public or package private
     */
    public static int getVisibilityAccessFlagForClass(@NotNull ClassDescriptor descriptor) {
        if (descriptor instanceof SyntheticClassDescriptorForLambda) {
            return getVisibilityAccessFlagForAnonymous(descriptor);
        }
        if (descriptor.getKind() == ClassKind.ENUM_ENTRY) {
            return NO_FLAG_PACKAGE_PRIVATE;
        }
        if (descriptor.getVisibility() == DescriptorVisibilities.PUBLIC ||
            descriptor.getVisibility() == DescriptorVisibilities.PROTECTED ||
            // TODO: should be package private, but for now Kotlin's reflection can't access members of such classes
            descriptor.getVisibility() == DescriptorVisibilities.LOCAL ||
            descriptor.getVisibility() == DescriptorVisibilities.INTERNAL) {
            return ACC_PUBLIC;
        }
        return NO_FLAG_PACKAGE_PRIVATE;
    }

    private static int getVisibilityAccessFlagForAnonymous(@NotNull ClassDescriptor descriptor) {
        return InlineUtil.isInlineOrContainingInline(descriptor.getContainingDeclaration()) ? ACC_PUBLIC : NO_FLAG_PACKAGE_PRIVATE;
    }

    public static int getSyntheticAccessFlagForLambdaClass(@NotNull ClassDescriptor descriptor) {
        return descriptor instanceof SyntheticClassDescriptorForLambda &&
               ((SyntheticClassDescriptorForLambda) descriptor).isCallableReference() ? ACC_SYNTHETIC : 0;
    }

    public static int calculateInnerClassAccessFlags(@NotNull ClassDescriptor innerClass) {
        int visibility =
                innerClass instanceof SyntheticClassDescriptorForLambda
                ? getVisibilityAccessFlagForAnonymous(innerClass)
                : innerClass.getVisibility() == DescriptorVisibilities.LOCAL
                  ? ACC_PUBLIC
                  : getVisibilityAccessFlag(innerClass);
        return visibility |
               getSyntheticAccessFlagForLambdaClass(innerClass) |
               innerAccessFlagsForModalityAndKind(innerClass) |
               (innerClass.isInner() ? 0 : ACC_STATIC);
    }

    private static int innerAccessFlagsForModalityAndKind(@NotNull ClassDescriptor innerClass) {
        switch (innerClass.getKind()) {
            case INTERFACE:
                return ACC_ABSTRACT | ACC_INTERFACE;
            case ENUM_CLASS:
                return ACC_FINAL | ACC_ENUM;
            case ANNOTATION_CLASS:
                return ACC_ABSTRACT | ACC_ANNOTATION | ACC_INTERFACE;
            default:
                Modality modality = innerClass.getModality();
                if (modality == Modality.FINAL) {
                    return ACC_FINAL;
                }
                else if (modality == Modality.ABSTRACT || modality == Modality.SEALED) {
                    return ACC_ABSTRACT;
                }
        }
        return 0;
    }

    public static int getDeprecatedAccessFlag(@NotNull MemberDescriptor descriptor) {
        if (descriptor instanceof PropertyAccessorDescriptor) {
            return KotlinBuiltIns.isDeprecated(descriptor)
                   ? ACC_DEPRECATED
                   : getDeprecatedAccessFlag(((PropertyAccessorDescriptor) descriptor).getCorrespondingProperty());
        }
        else if (KotlinBuiltIns.isDeprecated(descriptor)) {
            return ACC_DEPRECATED;
        }
        return 0;
    }

    private static int getVarargsFlag(FunctionDescriptor functionDescriptor) {
        if (!functionDescriptor.getValueParameters().isEmpty()
            && functionDescriptor.getValueParameters().get(functionDescriptor.getValueParameters().size() - 1)
                       .getVarargElementType() != null) {
            return ACC_VARARGS;
        }
        return 0;
    }

    @Nullable
    private static Integer specialCaseVisibility(@NotNull MemberDescriptor memberDescriptor, @Nullable OwnerKind kind) {
        DeclarationDescriptor containingDeclaration = memberDescriptor.getContainingDeclaration();
        DescriptorVisibility memberVisibility = memberDescriptor.getVisibility();

        if (JvmCodegenUtil.isNonIntrinsicPrivateCompanionObjectInInterface(memberDescriptor)) {
            return ACC_PUBLIC;
        }

        if (memberDescriptor instanceof FunctionDescriptor &&
            isInlineClassWrapperConstructor((FunctionDescriptor) memberDescriptor, kind)) {
            return ACC_PRIVATE;
        }

        if (kind != OwnerKind.ERASED_INLINE_CLASS &&
            memberDescriptor instanceof ConstructorDescriptor &&
            !(memberDescriptor instanceof AccessorForConstructorDescriptor) &&
            InlineClassManglingRulesKt.shouldHideConstructorDueToValueClassTypeValueParameters((ConstructorDescriptor) memberDescriptor)
        ) {
            return ACC_PRIVATE;
        }

        // Sealed class constructors should be ACC_PRIVATE.
        // In 1.4 and before, sealed class constructors had PRIVATE visibility, and were represented as private methods in bytecode.
        // In 1.5 (+AllowSealedInheritorsInDifferentFilesOfSamePackage), sealed class constructors became INTERNAL,
        // but still should be represented as private methods in bytecode in order to prevent inheriting from sealed classes on JVM.
        if (memberDescriptor instanceof ConstructorDescriptor &&
            !(memberDescriptor instanceof AccessorForConstructorDescriptor) &&
            isSealedClass(((ConstructorDescriptor) memberDescriptor).getConstructedClass()) &&
            memberDescriptor.getVisibility() != DescriptorVisibilities.PUBLIC
        ) {
            return ACC_PRIVATE;
        }

        if (isInlineOnlyPrivateInBytecode(memberDescriptor)) {
            return ACC_PRIVATE;
        }

        if (memberVisibility == DescriptorVisibilities.LOCAL && memberDescriptor instanceof CallableMemberDescriptor) {
            return ACC_PUBLIC;
        }

        if (isEnumEntry(memberDescriptor)) {
            return NO_FLAG_PACKAGE_PRIVATE;
        }

        if (isToArrayFromCollection(memberDescriptor)) {
            return ACC_PUBLIC;
        }

        if (memberDescriptor instanceof ConstructorDescriptor && isAnonymousObject(memberDescriptor.getContainingDeclaration())) {
            return getVisibilityAccessFlagForAnonymous((ClassDescriptor) memberDescriptor.getContainingDeclaration());
        }

        if (memberDescriptor instanceof SyntheticJavaPropertyDescriptor) {
            return getVisibilityAccessFlag(((SyntheticJavaPropertyDescriptor) memberDescriptor).getGetMethod());
        }
        if (memberDescriptor instanceof PropertyAccessorDescriptor) {
            PropertyDescriptor property = ((PropertyAccessorDescriptor) memberDescriptor).getCorrespondingProperty();
            if (property instanceof SyntheticJavaPropertyDescriptor) {
                FunctionDescriptor method = memberDescriptor == property.getGetter()
                                            ? ((SyntheticJavaPropertyDescriptor) property).getGetMethod()
                                            : ((SyntheticJavaPropertyDescriptor) property).getSetMethod();
                assert method != null : "No get/set method in SyntheticJavaPropertyDescriptor: " + property;
                return getVisibilityAccessFlag(method);
            }
        }

        if (memberDescriptor instanceof CallableDescriptor && memberVisibility == DescriptorVisibilities.PROTECTED) {
            for (CallableDescriptor overridden : DescriptorUtils.getAllOverriddenDescriptors((CallableDescriptor) memberDescriptor)) {
                if (isJvmInterface(overridden.getContainingDeclaration())) {
                    return ACC_PUBLIC;
                }
            }
        }

        if (!DescriptorVisibilities.isPrivate(memberVisibility)) {
            return null;
        }

        if (memberDescriptor instanceof AccessorForCompanionObjectInstanceFieldDescriptor) {
            return NO_FLAG_PACKAGE_PRIVATE;
        }

        if (memberDescriptor instanceof ConstructorDescriptor && isEnumEntry(containingDeclaration)) {
            return NO_FLAG_PACKAGE_PRIVATE;
        }

        return null;
    }

    public static void genClosureFields(
            @NotNull CalculatedClosure closure,
            ClassBuilder v,
            KotlinTypeMapper typeMapper,
            @NotNull LanguageVersionSettings languageVersionSettings
    ) {
        List<Pair<String, Type>> allFields = new ArrayList<>();

        ClassifierDescriptor captureThis = closure.getCapturedOuterClassDescriptor();
        if (captureThis != null) {
            allFields.add(Pair.create(CAPTURED_THIS_FIELD, typeMapper.mapType(captureThis)));
        }

        KotlinType captureReceiverType = closure.getCapturedReceiverFromOuterContext();
        if (captureReceiverType != null && !CallableReferenceUtilKt.isForCallableReference(closure)) {
            String fieldName = closure.getCapturedReceiverFieldName(typeMapper.getBindingContext(), languageVersionSettings);
            allFields.add(Pair.create(fieldName, typeMapper.mapType(captureReceiverType)));
        }

        allFields.addAll(closure.getRecordedFields());
        genClosureFields(allFields, v);
    }

    public static void genClosureFields(List<Pair<String, Type>> allFields, ClassBuilder builder) {
        int access = NO_FLAG_PACKAGE_PRIVATE | ACC_SYNTHETIC | ACC_FINAL;
        for (Pair<String, Type> field : allFields) {
            builder.newField(JvmDeclarationOrigin.NO_ORIGIN, access, field.first, field.second.getDescriptor(), null, null);
        }
    }

    public static int genAssignInstanceFieldFromParam(FieldInfo info, int index, InstructionAdapter iv) {
        return genAssignInstanceFieldFromParam(info, index, iv, 0, false);
    }

    public static int genAssignInstanceFieldFromParam(
            FieldInfo info,
            int index,
            InstructionAdapter iv,
            int ownerIndex,
            boolean cast
    ) {
        assert !info.isStatic();
        Type fieldType = info.getFieldType();
        KotlinType fieldKotlinType = info.getFieldKotlinType();
        KotlinType nullableAny;
        if (fieldKotlinType != null) {
            nullableAny = fieldKotlinType.getConstructor().getBuiltIns().getNullableAnyType();
        } else {
            nullableAny = null;
        }

        iv.load(ownerIndex, info.getOwnerType());//this
        if (cast) {
            iv.load(index, AsmTypes.OBJECT_TYPE); //param
            StackValue.coerce(AsmTypes.OBJECT_TYPE, nullableAny, fieldType, fieldKotlinType, iv);
        } else {
            iv.load(index, fieldType); //param
        }
        iv.visitFieldInsn(PUTFIELD, info.getOwnerInternalName(), info.getFieldName(), fieldType.getDescriptor());
        index += fieldType.getSize();
        return index;
    }

    public static void genInvokeAppendMethod(
            @NotNull StringConcatGenerator generator,
            @NotNull Type type,
            @Nullable KotlinType kotlinType,
            @Nullable KotlinTypeMapper typeMapper,
            @NotNull StackValue stackValue
    ) {
        CallableMethod specializedToString = getSpecializedToStringCallableMethodOrNull(kotlinType, typeMapper);
        if (specializedToString != null) {
            stackValue.put(type, kotlinType, generator.getMv());
            specializedToString.genInvokeInstruction(generator.getMv());
            generator.invokeAppend(AsmTypes.JAVA_STRING_TYPE);
        }
        else if (kotlinType != null && InlineClassesUtilsKt.isInlineClassType(kotlinType)) {
            SimpleType nullableAnyType = kotlinType.getConstructor().getBuiltIns().getNullableAnyType();
            stackValue.put(type, kotlinType, generator.getMv());
            StackValue.coerce(type, kotlinType, OBJECT_TYPE, nullableAnyType, generator.getMv());
            generator.invokeAppend(OBJECT_TYPE);
        }
        else {
            generator.putValueOrProcessConstant(stackValue, type, kotlinType);
        }
    }

    public static StackValue genToString(
            @NotNull StackValue receiver,
            @NotNull Type receiverType,
            @Nullable KotlinType receiverKotlinType,
            @Nullable KotlinTypeMapper typeMapper
    ) {
        return StackValue.operation(JAVA_STRING_TYPE, v -> {
            CallableMethod specializedToString = getSpecializedToStringCallableMethodOrNull(receiverKotlinType, typeMapper);
            if (specializedToString != null) {
                receiver.put(receiverType, receiverKotlinType, v);
                specializedToString.genInvokeInstruction(v);
                return null;
            }

            Type type;
            KotlinType kotlinType;
            if (receiverKotlinType != null && InlineClassesUtilsKt.isInlineClassType(receiverKotlinType)) {
                type = OBJECT_TYPE;
                kotlinType = receiverKotlinType.getConstructor().getBuiltIns().getNullableAnyType();
            }
            else {
                type = stringValueOfType(receiverType);
                kotlinType = null;
            }

            receiver.put(type, kotlinType, v);
            v.invokestatic("java/lang/String", "valueOf", "(" + type.getDescriptor() + ")Ljava/lang/String;", false);
            return null;
        });
    }

    @Nullable
    private static CallableMethod getSpecializedToStringCallableMethodOrNull(
            @Nullable KotlinType receiverKotlinType,
            @Nullable KotlinTypeMapper typeMapper
    ) {
        if (typeMapper == null) return null;

        if (receiverKotlinType == null) return null;
        if (!InlineClassesUtilsKt.isInlineClassType(receiverKotlinType)) return null;
        if (receiverKotlinType.isMarkedNullable()) return null;

        DeclarationDescriptor receiverTypeDescriptor = receiverKotlinType.getConstructor().getDeclarationDescriptor();
        assert receiverTypeDescriptor != null && InlineClassesUtilsKt.isInlineClass(receiverTypeDescriptor) :
                "Inline class type expected: " + receiverKotlinType;
        ClassDescriptor receiverClassDescriptor = (ClassDescriptor) receiverTypeDescriptor;
        FunctionDescriptor toStringDescriptor = receiverClassDescriptor.getUnsubstitutedMemberScope()
                .getContributedFunctions(Name.identifier("toString"), NoLookupLocation.FROM_BACKEND)
                .stream()
                .filter(
                        f -> f.getValueParameters().size() == 0
                             && KotlinBuiltIns.isString(f.getReturnType())
                             && f.getDispatchReceiverParameter() != null
                             && f.getExtensionReceiverParameter() == null
                )
                .findFirst()
                .orElseThrow(() -> new AssertionError("'toString' not found in member scope of " + receiverClassDescriptor));

        return typeMapper.mapToCallableMethod(toStringDescriptor, false, OwnerKind.ERASED_INLINE_CLASS);
    }

    public static void genHashCode(MethodVisitor mv, InstructionAdapter iv, Type type, JvmTarget jvmTarget) {
        if (type.getSort() == Type.ARRAY) {
            Type elementType = correctElementType(type);
            if (elementType.getSort() == Type.OBJECT || elementType.getSort() == Type.ARRAY) {
                iv.invokestatic("java/util/Arrays", "hashCode", "([Ljava/lang/Object;)I", false);
            }
            else {
                iv.invokestatic("java/util/Arrays", "hashCode", "(" + type.getDescriptor() + ")I", false);
            }
        }
        else if (type.getSort() == Type.OBJECT) {
            iv.invokevirtual("java/lang/Object", "hashCode", "()I", false);
        }
        else {
            HashCode.Companion.invokeHashCode(iv, type);
        }
    }

    @NotNull
    public static StackValue genEqualsForExpressionsOnStack(
            @NotNull IElementType opToken,
            @NotNull StackValue left,
            @NotNull StackValue right
    ) {
        Type leftType = left.type;
        Type rightType = right.type;
        if (isPrimitive(leftType) && leftType == rightType) {
            return StackValue.cmp(opToken, leftType, left, right);
        }

        return StackValue.operation(Type.BOOLEAN_TYPE, v -> {
            left.put(AsmTypes.OBJECT_TYPE, left.kotlinType, v);
            right.put(AsmTypes.OBJECT_TYPE, right.kotlinType, v);
            return genAreEqualCall(v, opToken);
        });
    }

    @NotNull
    public static BranchedValue genTotalOrderEqualsForExpressionOnStack(
            @NotNull StackValue left,
            @NotNull StackValue right,
            @NotNull Type asmType
    ) {
        return new BranchedValue(left, right, asmType, Opcodes.IFEQ) {
            @Override
            public void condJump(@NotNull Label jumpLabel, @NotNull InstructionAdapter iv, boolean jumpIfFalse) {
                if (asmType.getSort() == Type.FLOAT) {
                    left.put(asmType, kotlinType, iv);
                    right.put(asmType, kotlinType, iv);
                    iv.invokestatic("java/lang/Float", "compare", "(FF)I", false);
                    iv.visitJumpInsn(patchOpcode(jumpIfFalse ? Opcodes.IFNE : Opcodes.IFEQ, iv), jumpLabel);
                } else if (asmType.getSort() == Type.DOUBLE) {
                    left.put(asmType, kotlinType, iv);
                    right.put(asmType, kotlinType, iv);
                    iv.invokestatic("java/lang/Double", "compare", "(DD)I", false);
                    iv.visitJumpInsn(patchOpcode(jumpIfFalse ? Opcodes.IFNE : Opcodes.IFEQ, iv), jumpLabel);
                } else {
                    StackValue value = genEqualsForExpressionsOnStack(KtTokens.EQEQ, left, right);
                    BranchedValue.Companion.condJump(value, jumpLabel, jumpIfFalse, iv);
                }
            }
        };
    }

    @NotNull
    public static StackValue genEqualsBoxedOnStack(@NotNull IElementType opToken) {
        return StackValue.operation(Type.BOOLEAN_TYPE, v -> genAreEqualCall(v, opToken));
    }

    public static void genAreEqualCall(InstructionAdapter v) {
        v.invokestatic(IntrinsicMethods.INTRINSICS_CLASS_NAME, "areEqual", "(Ljava/lang/Object;Ljava/lang/Object;)Z", false);
    }

    @NotNull
    private static Unit genAreEqualCall(InstructionAdapter v, @NotNull IElementType opToken) {
        genAreEqualCall(v);

        if (opToken == KtTokens.EXCLEQ || opToken == KtTokens.EXCLEQEQEQ) {
            genInvertBoolean(v);
        }

        return Unit.INSTANCE;
    }

    public static void genIEEE754EqualForNullableTypesCall(InstructionAdapter v, Type left, Type right) {
        v.invokestatic(IntrinsicMethods.INTRINSICS_CLASS_NAME, "areEqual", "(" + left.getDescriptor() + right.getDescriptor() + ")Z", false);
    }

    public static void genIncrement(Type baseType, int myDelta, InstructionAdapter v) {
        Type operationType = numberFunctionOperandType(baseType);
        numConst(myDelta, operationType, v);
        v.add(operationType);
        StackValue.coerce(operationType, baseType, v);
    }

    static void genNotNullAssertionsForParameters(
            @NotNull InstructionAdapter v,
            @NotNull GenerationState state,
            @NotNull FunctionDescriptor descriptor,
            @NotNull FrameMap frameMap
    ) {
        if (state.getConfig().isParamAssertionsDisabled()) return;
        // currently when resuming a suspend function we pass default values instead of real arguments (i.e. nulls for references)
        if (descriptor.isSuspend()) return;

        if (getVisibilityAccessFlag(descriptor) == ACC_PRIVATE) {
            // Private method is not accessible from other classes, no assertions needed,
            // unless we have a private operator function, in which we should generate a parameter assertion for an extension receiver.

            // HACK: this provides "fail fast" behavior for operator functions.
            // Such functions can be invoked in operator conventions desugaring,
            // which is currently done on ad hoc basis in ExpressionCodegen.

            if (state.getConfig().isReceiverAssertionsDisabled()) return;
            if (descriptor.isOperator()) {
                ReceiverParameterDescriptor receiverParameter = descriptor.getExtensionReceiverParameter();
                if (receiverParameter != null) {
                    String name = getNameForReceiverParameter(descriptor, state.getBindingContext(), state.getLanguageVersionSettings());
                    genParamAssertion(v, state, frameMap, receiverParameter, name, descriptor);
                }
            }
            return;
        }

        ReceiverParameterDescriptor receiverParameter = descriptor.getExtensionReceiverParameter();
        if (receiverParameter != null) {
            String name = getNameForReceiverParameter(descriptor, state.getBindingContext(), state.getLanguageVersionSettings());
            genParamAssertion(v, state, frameMap, receiverParameter, name, descriptor);
        }

        for (ValueParameterDescriptor parameter : descriptor.getValueParameters()) {
            genParamAssertion(v, state, frameMap, parameter, parameter.getName().asString(), descriptor);
        }
    }

    private static void genParamAssertion(
            @NotNull InstructionAdapter v,
            @NotNull GenerationState state,
            @NotNull FrameMap frameMap,
            @NotNull ParameterDescriptor parameter,
            @NotNull String name,
            @NotNull FunctionDescriptor containingDeclaration
    ) {
        KotlinType type = parameter.getType();
        if (isNullableType(type) || InlineClassesUtilsKt.isNullableUnderlyingType(type)) return;

        Type asmType = state.getTypeMapper().mapType(type);
        if (asmType.getSort() == Type.OBJECT || asmType.getSort() == Type.ARRAY) {
            StackValue value;
            if (JvmCodegenUtil.isDeclarationOfBigArityFunctionInvoke(containingDeclaration) ||
                JvmCodegenUtil.isDeclarationOfBigArityCreateCoroutineMethod(containingDeclaration)) {
                int index = getIndexOfParameterInVarargInvokeArray(parameter);
                value = StackValue.arrayElement(
                        OBJECT_TYPE, null, StackValue.local(1, getArrayType(OBJECT_TYPE)), StackValue.constant(index)
                );
            }
            else {
                int index = frameMap.getIndex(parameter);
                value = StackValue.local(index, asmType);
            }
            value.put(asmType, v);
            v.visitLdcInsn(name);
            String methodName = state.getConfig().getUnifiedNullChecks() ? "checkNotNullParameter" : "checkParameterIsNotNull";
            v.invokestatic(IntrinsicMethods.INTRINSICS_CLASS_NAME, methodName, "(Ljava/lang/Object;Ljava/lang/String;)V", false);
        }
    }

    @NotNull
    public static StackValue genNotNullAssertions(
            @NotNull GenerationState state,
            @NotNull StackValue stackValue,
            @Nullable RuntimeAssertionInfo runtimeAssertionInfo
    ) {
        if (state.getConfig().isCallAssertionsDisabled()) return stackValue;
        if (runtimeAssertionInfo == null || !runtimeAssertionInfo.getNeedNotNullAssertion()) return stackValue;

        return new StackValue(stackValue.type, stackValue.kotlinType) {
            @Override
            public void putSelector(@NotNull Type type, @Nullable KotlinType kotlinType, @NotNull InstructionAdapter v) {
                Type innerType = stackValue.type;
                KotlinType innerKotlinType = stackValue.kotlinType;
                stackValue.put(innerType, innerKotlinType, v);
                if (innerType.getSort() == Type.OBJECT || innerType.getSort() == Type.ARRAY) {
                    v.dup();
                    v.visitLdcInsn(runtimeAssertionInfo.getMessage());
                    String methodName =
                            state.getConfig().getUnifiedNullChecks() ? "checkNotNullExpressionValue" : "checkExpressionValueIsNotNull";
                    v.invokestatic(IntrinsicMethods.INTRINSICS_CLASS_NAME, methodName, "(Ljava/lang/Object;Ljava/lang/String;)V", false);
                }
                StackValue.coerce(innerType, innerKotlinType, type, kotlinType, v);
            }
        };
    }

    private static int getIndexOfParameterInVarargInvokeArray(@NotNull ParameterDescriptor parameter) {
        if (parameter instanceof ReceiverParameterDescriptor) return 0;

        DeclarationDescriptor container = parameter.getContainingDeclaration();
        assert parameter instanceof ValueParameterDescriptor : "Non-extension-receiver parameter must be a value parameter: " + parameter;
        int extensionShift = ((CallableDescriptor) container).getExtensionReceiverParameter() == null ? 0 : 1;

        return extensionShift + ((ValueParameterDescriptor) parameter).getIndex();
    }

    // At the beginning of the vararg invoke of a function with big arity N, generates an assert that the vararg parameter has N elements
    public static void generateVarargInvokeArityAssert(InstructionAdapter v, int functionArity) {
        Label start = new Label();
        v.load(1, getArrayType(OBJECT_TYPE));
        v.arraylength();
        v.iconst(functionArity);
        v.ificmpeq(start);
        v.visitLdcInsn("Vararg argument must contain " + functionArity + " elements.");
        v.invokestatic(IntrinsicMethods.INTRINSICS_CLASS_NAME, "throwIllegalArgument", "(Ljava/lang/String;)V", false);
        v.visitLabel(start);
    }

    public static boolean isInstancePropertyWithStaticBackingField(@NotNull PropertyDescriptor propertyDescriptor) {
        return propertyDescriptor.getKind() != CallableMemberDescriptor.Kind.FAKE_OVERRIDE &&
               isObject(propertyDescriptor.getContainingDeclaration());
    }

    public static int getVisibilityForBackingField(@NotNull PropertyDescriptor propertyDescriptor, boolean isDelegate) {
        boolean isExtensionProperty = propertyDescriptor.getExtensionReceiverParameter() != null;
        if (isDelegate || isExtensionProperty) {
            return ACC_PRIVATE;
        }
        else {
            return propertyDescriptor.isLateInit() || isConstOrHasJvmFieldAnnotation(propertyDescriptor)
                   ? getVisibilityAccessFlag(descriptorForVisibility(propertyDescriptor))
                   : ACC_PRIVATE;
        }
    }

    private static MemberDescriptor descriptorForVisibility(@NotNull PropertyDescriptor propertyDescriptor) {
        if (!propertyDescriptor.isVar()) {
            return propertyDescriptor;
        }
        else {
            return propertyDescriptor.getSetter() != null ? propertyDescriptor.getSetter() : propertyDescriptor;
        }
    }

    public static boolean isPropertyWithBackingFieldCopyInOuterClass(@NotNull PropertyDescriptor propertyDescriptor) {
        DeclarationDescriptor propertyContainer = propertyDescriptor.getContainingDeclaration();
        return propertyDescriptor.isConst()
               && isCompanionObject(propertyContainer) && isJvmInterface(propertyContainer.getContainingDeclaration())
               && getVisibilityForBackingField(propertyDescriptor, false) == ACC_PUBLIC;
    }

    public static void writeAnnotationData(
            @NotNull AnnotationVisitor av,
            @NotNull DescriptorSerializer serializer,
            @NotNull MessageLite message
    ) {
        writeAnnotationData(av, message, (JvmStringTable) serializer.getStringTable());
    }

    public static void writeAnnotationData(
            @NotNull AnnotationVisitor av, @NotNull MessageLite message, @NotNull JvmStringTable stringTable
    ) {
        AsmUtil.writeAnnotationData(av, JvmProtoBufUtil.writeData(message, stringTable), ArrayUtil.toStringArray(stringTable.getStrings()));
    }

    public static void putJavaLangClassInstance(
            @NotNull InstructionAdapter v,
            @NotNull Type type,
            @Nullable KotlinType kotlinType,
            @NotNull KotlinTypeMapper typeMapper
    ) {
        if (kotlinType != null && InlineClassesUtilsKt.isInlineClassType(kotlinType)) {
            v.aconst(boxType(type, kotlinType, typeMapper));
        }
        else if (isPrimitive(type)) {
            v.getstatic(AsmUtil.boxType(type).getInternalName(), "TYPE", "Ljava/lang/Class;");
        }
        else {
            v.aconst(type);
        }
    }

    public static int getReceiverIndex(@NotNull CodegenContext context, @NotNull CallableMemberDescriptor descriptor) {
        OwnerKind kind = context.getContextKind();
        //Trait always should have this descriptor
        return kind != OwnerKind.DEFAULT_IMPLS && isStaticMethod(kind, descriptor) ? 0 : 1;
    }

    public static boolean isHiddenConstructor(FunctionDescriptor descriptor) {
        if (!(descriptor instanceof ClassConstructorDescriptor)) return false;

        ClassConstructorDescriptor classConstructorDescriptor = (ClassConstructorDescriptor) descriptor;
        if (InlineClassManglingRulesKt.shouldHideConstructorDueToValueClassTypeValueParameters(descriptor)) {
            return true;
        }
        if (isSealedClass(classConstructorDescriptor.getConstructedClass()) &&
            classConstructorDescriptor.getVisibility() != DescriptorVisibilities.PUBLIC
        ) {
            return true;
        }
        return false;
    }
}
