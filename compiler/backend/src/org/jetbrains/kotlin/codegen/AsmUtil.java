/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.ArrayUtil;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.builtins.PrimitiveType;
import org.jetbrains.kotlin.codegen.binding.CalculatedClosure;
import org.jetbrains.kotlin.codegen.binding.CodegenBinding;
import org.jetbrains.kotlin.codegen.context.CodegenContext;
import org.jetbrains.kotlin.codegen.intrinsics.HashCode;
import org.jetbrains.kotlin.codegen.intrinsics.IntrinsicMethods;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper;
import org.jetbrains.kotlin.resolve.JvmTarget;
import org.jetbrains.kotlin.config.LanguageFeature;
import org.jetbrains.kotlin.config.LanguageVersionSettings;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.incremental.components.NoLookupLocation;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.load.java.JavaVisibilities;
import org.jetbrains.kotlin.load.java.JvmAnnotationNames;
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil;
import org.jetbrains.kotlin.metadata.jvm.serialization.JvmStringTable;
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.protobuf.MessageLite;
import org.jetbrains.kotlin.renderer.DescriptorRenderer;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.InlineClassDescriptorResolver;
import org.jetbrains.kotlin.resolve.InlineClassesUtilsKt;
import org.jetbrains.kotlin.resolve.checkers.ExpectedActualDeclarationChecker;
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver;
import org.jetbrains.kotlin.resolve.inline.InlineUtil;
import org.jetbrains.kotlin.resolve.jvm.*;
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin;
import org.jetbrains.kotlin.serialization.DescriptorSerializer;
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.SimpleType;
import org.jetbrains.org.objectweb.asm.*;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;
import org.jetbrains.org.objectweb.asm.commons.Method;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jetbrains.kotlin.builtins.KotlinBuiltIns.isBoolean;
import static org.jetbrains.kotlin.builtins.KotlinBuiltIns.isPrimitiveClass;
import static org.jetbrains.kotlin.codegen.CodegenUtilKt.isToArrayFromCollection;
import static org.jetbrains.kotlin.codegen.JvmCodegenUtil.isConstOrHasJvmFieldAnnotation;
import static org.jetbrains.kotlin.codegen.JvmCodegenUtil.isJvmInterface;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.*;
import static org.jetbrains.kotlin.resolve.inline.InlineOnlyKt.isEffectivelyInlineOnly;
import static org.jetbrains.kotlin.resolve.jvm.AsmTypes.*;
import static org.jetbrains.kotlin.resolve.jvm.annotations.JvmAnnotationUtilKt.hasJvmDefaultAnnotation;
import static org.jetbrains.kotlin.resolve.jvm.annotations.JvmAnnotationUtilKt.hasJvmSyntheticAnnotation;
import static org.jetbrains.kotlin.types.TypeUtils.isNullableType;
import static org.jetbrains.org.objectweb.asm.Opcodes.*;

public class AsmUtil {

    public static final boolean IS_BUILT_WITH_ASM6 = Opcodes.API_VERSION <= Opcodes.ASM6;

    private static final Set<Type> STRING_BUILDER_OBJECT_APPEND_ARG_TYPES = Sets.newHashSet(
            getType(String.class),
            getType(StringBuffer.class),
            getType(CharSequence.class)
    );

    private static final int NO_FLAG_LOCAL = 0;
    public static final int NO_FLAG_PACKAGE_PRIVATE = 0;

    @NotNull
    private static final Map<Visibility, Integer> visibilityToAccessFlag = ImmutableMap.<Visibility, Integer>builder()
            .put(Visibilities.PRIVATE, ACC_PRIVATE)
            .put(Visibilities.PRIVATE_TO_THIS, ACC_PRIVATE)
            .put(Visibilities.PROTECTED, ACC_PROTECTED)
            .put(JavaVisibilities.PROTECTED_STATIC_VISIBILITY, ACC_PROTECTED)
            .put(JavaVisibilities.PROTECTED_AND_PACKAGE, ACC_PROTECTED)
            .put(Visibilities.PUBLIC, ACC_PUBLIC)
            .put(Visibilities.INTERNAL, ACC_PUBLIC)
            .put(Visibilities.LOCAL, NO_FLAG_LOCAL)
            .put(JavaVisibilities.PACKAGE_VISIBILITY, NO_FLAG_PACKAGE_PRIVATE)
            .build();

    public static final String THIS = "this";

    public static final String THIS_IN_DEFAULT_IMPLS = "$this";

    public static final String LABELED_THIS_FIELD = THIS + "_";

    public static final String LABELED_THIS_PARAMETER = "$" + THIS + "$";

    public static final String CAPTURED_THIS_FIELD = "this$0";

    public static final String RECEIVER_PARAMETER_NAME = "$receiver";

    /*
        This is basically an old convention. Starting from Kotlin 1.3, it was replaced with `$this_<label>`.
        Note that it is still used for inlined callable references and anonymous callable extension receivers
        even in 1.3.
    */
    public static final String CAPTURED_RECEIVER_FIELD = "receiver$0";

    // For non-inlined callable references ('kotlin.jvm.internal.CallableReference' has a 'receiver' field)
    public static final String BOUND_REFERENCE_RECEIVER = "receiver";

    public static final String LOCAL_FUNCTION_VARIABLE_PREFIX = "$fun$";

    private static final ImmutableMap<Integer, JvmPrimitiveType> primitiveTypeByAsmSort;
    private static final ImmutableMap<Type, Type> primitiveTypeByBoxedType;

    static {
        ImmutableMap.Builder<Integer, JvmPrimitiveType> typeBySortBuilder = ImmutableMap.builder();
        ImmutableMap.Builder<Type, Type> typeByWrapperBuilder = ImmutableMap.builder();
        for (JvmPrimitiveType primitiveType : JvmPrimitiveType.values()) {
            Type asmType = Type.getType(primitiveType.getDesc());
            typeBySortBuilder.put(asmType.getSort(), primitiveType);
            typeByWrapperBuilder.put(asmTypeByFqNameWithoutInnerClasses(primitiveType.getWrapperFqName()), asmType);
        }
        primitiveTypeByAsmSort = typeBySortBuilder.build();
        primitiveTypeByBoxedType = typeByWrapperBuilder.build();
    }

    private AsmUtil() {
    }

    @NotNull
    public static String getCapturedFieldName(@NotNull String originalName) {
        return "$" + originalName;
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
    public static String getLabeledThisName(@NotNull String callableName, @NotNull String prefix, @NotNull String defaultName) {
        if (!Name.isValidIdentifier(callableName)) {
            return defaultName;
        }

        return prefix + VariableAsmNameManglingUtils.mangleNameIfNeeded(callableName);
    }

    @NotNull
    public static Type boxType(@NotNull Type type) {
        Type boxedType = boxPrimitiveType(type);
        return boxedType != null ? boxedType : type;
    }

    @NotNull
    public static Type boxType(@NotNull Type type, @NotNull KotlinType kotlinType, @NotNull KotlinTypeMapper typeMapper) {
        if (InlineClassesUtilsKt.isInlineClassType(kotlinType)) {
            return typeMapper.mapTypeAsDeclaration(kotlinType);
        }

        Type boxedPrimitiveType = boxPrimitiveType(type);
        return boxedPrimitiveType != null ? boxedPrimitiveType : type;
    }

    @Nullable
    private static Type boxPrimitiveType(@NotNull Type type) {
        JvmPrimitiveType jvmPrimitiveType = primitiveTypeByAsmSort.get(type.getSort());
        return jvmPrimitiveType != null ? asmTypeByFqNameWithoutInnerClasses(jvmPrimitiveType.getWrapperFqName()) : null;
    }

    @NotNull
    public static Type unboxType(@NotNull Type boxedType) {
        Type primitiveType = unboxPrimitiveTypeOrNull(boxedType);
        if (primitiveType == null) {
            throw new UnsupportedOperationException("Unboxing: " + boxedType);
        }
        return primitiveType;
    }

    @Nullable
    public static Type unboxPrimitiveTypeOrNull(@NotNull Type boxedType) {
        return primitiveTypeByBoxedType.get(boxedType);
    }

    public static boolean isBoxedPrimitiveType(@NotNull Type boxedType) {
        return primitiveTypeByBoxedType.get(boxedType) != null;
    }

    @NotNull
    public static Type unboxUnlessPrimitive(@NotNull Type boxedOrPrimitiveType) {
        if (isPrimitive(boxedOrPrimitiveType)) return boxedOrPrimitiveType;
        return unboxType(boxedOrPrimitiveType);
    }

    public static boolean isBoxedTypeOf(@NotNull Type boxedType, @NotNull Type unboxedType) {
        return unboxPrimitiveTypeOrNull(boxedType) == unboxedType;
    }

    public static boolean isIntPrimitive(Type type) {
        return type == Type.INT_TYPE || type == Type.SHORT_TYPE || type == Type.BYTE_TYPE || type == Type.CHAR_TYPE;
    }

    public static boolean isIntOrLongPrimitive(Type type) {
        return isIntPrimitive(type) || type == Type.LONG_TYPE;
    }

    public static boolean isPrimitive(Type type) {
        return type.getSort() != Type.OBJECT && type.getSort() != Type.ARRAY;
    }

    public static boolean isPrimitiveNumberClassDescriptor(DeclarationDescriptor descriptor) {
        if (!(descriptor instanceof ClassDescriptor)) {
            return false;
        }
        return isPrimitiveClass((ClassDescriptor) descriptor) && !isBoolean((ClassDescriptor) descriptor);
    }

    @NotNull
    public static Type correctElementType(@NotNull Type type) {
        String internalName = type.getInternalName();
        assert internalName.charAt(0) == '[';
        return Type.getType(internalName.substring(1));
    }

    @NotNull
    public static Type getArrayType(@NotNull Type componentType) {
        return Type.getType("[" + componentType.getDescriptor());
    }

    @Nullable
    public static PrimitiveType asmPrimitiveTypeToLangPrimitiveType(Type type) {
        JvmPrimitiveType jvmPrimitiveType = primitiveTypeByAsmSort.get(type.getSort());
        return jvmPrimitiveType != null ? jvmPrimitiveType.getPrimitiveType() : null;
    }

    @NotNull
    public static Method method(@NotNull String name, @NotNull Type returnType, @NotNull Type... parameterTypes) {
        return new Method(name, Type.getMethodDescriptor(returnType, parameterTypes));
    }

    public static boolean isAbstractMethod(FunctionDescriptor functionDescriptor, OwnerKind kind) {
        return (functionDescriptor.getModality() == Modality.ABSTRACT ||
                (isJvmInterface(functionDescriptor.getContainingDeclaration()) && !hasJvmDefaultAnnotation(functionDescriptor)))
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
        return getMethodAsmFlags(functionDescriptor, kind, state.getDeprecationProvider());
    }

    public static int getMethodAsmFlags(FunctionDescriptor functionDescriptor, OwnerKind kind, DeprecationResolver deprecationResolver) {
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
            if (!(containingDeclaration instanceof ClassDescriptor) ||
                ((ClassDescriptor) containingDeclaration).getKind() != ClassKind.INTERFACE) {
                flags |= ACC_FINAL;
            }
        }

        if (isStaticMethod(kind, functionDescriptor)) {
            flags |= ACC_STATIC;
        }

        if (isAbstractMethod(functionDescriptor, kind)) {
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
        return classDescriptor.isInline() && kind == OwnerKind.IMPLEMENTATION;
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
        if (deprecationResolver.isDeprecatedHidden(functionDescriptor) ||
            (functionDescriptor.isSuspend()) && functionDescriptor.getVisibility().equals(Visibilities.PRIVATE)) {
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
        Visibility visibility = descriptor.getVisibility();
        Integer defaultMapping = getVisibilityAccessFlag(visibility);
        if (defaultMapping == null) {
            throw new IllegalStateException(visibility + " is not a valid visibility in backend for " + DescriptorRenderer.DEBUG_TEXT.render(descriptor));
        }
        return defaultMapping;
    }

    @Nullable
    public static Integer getVisibilityAccessFlag(Visibility visibility) {
        return visibilityToAccessFlag.get(visibility);
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
        if (ExpectedActualDeclarationChecker.isOptionalAnnotationClass(descriptor)) {
            return NO_FLAG_PACKAGE_PRIVATE;
        }
        if (descriptor.getKind() == ClassKind.ENUM_ENTRY) {
            return NO_FLAG_PACKAGE_PRIVATE;
        }
        if (descriptor.getVisibility() == Visibilities.PUBLIC ||
            descriptor.getVisibility() == Visibilities.PROTECTED ||
            // TODO: should be package private, but for now Kotlin's reflection can't access members of such classes
            descriptor.getVisibility() == Visibilities.LOCAL ||
            descriptor.getVisibility() == Visibilities.INTERNAL) {
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
                : innerClass.getVisibility() == Visibilities.LOCAL
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
        Visibility memberVisibility = memberDescriptor.getVisibility();

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
            InlineClassManglingRulesKt.shouldHideConstructorDueToInlineClassTypeValueParameters((ConstructorDescriptor) memberDescriptor)
        ) {
            return ACC_PRIVATE;
        }

        if (isEffectivelyInlineOnly(memberDescriptor)) {
            return ACC_PRIVATE;
        }

        if (memberVisibility == Visibilities.LOCAL && memberDescriptor instanceof CallableMemberDescriptor) {
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

        if (memberDescriptor instanceof CallableDescriptor && memberVisibility == Visibilities.PROTECTED) {
            for (CallableDescriptor overridden : DescriptorUtils.getAllOverriddenDescriptors((CallableDescriptor) memberDescriptor)) {
                if (isJvmInterface(overridden.getContainingDeclaration())) {
                    return ACC_PUBLIC;
                }
            }
        }

        if (!Visibilities.isPrivate(memberVisibility)) {
            return null;
        }

        if (memberDescriptor instanceof FunctionDescriptor && ((FunctionDescriptor) memberDescriptor).isSuspend()) {
            return NO_FLAG_PACKAGE_PRIVATE;
        }

        if (memberDescriptor instanceof AccessorForCompanionObjectInstanceFieldDescriptor) {
            return NO_FLAG_PACKAGE_PRIVATE;
        }

        if (memberDescriptor instanceof ConstructorDescriptor && isEnumEntry(containingDeclaration)) {
            return NO_FLAG_PACKAGE_PRIVATE;
        }

        return null;
    }

    public static Type stringValueOfType(Type type) {
        int sort = type.getSort();
        return sort == Type.OBJECT || sort == Type.ARRAY
               ? OBJECT_TYPE
               : sort == Type.BYTE || sort == Type.SHORT ? Type.INT_TYPE : type;
    }

    private static Type stringBuilderAppendType(Type type) {
        switch (type.getSort()) {
            case Type.OBJECT:
                return STRING_BUILDER_OBJECT_APPEND_ARG_TYPES.contains(type) ? type : OBJECT_TYPE;
            case Type.ARRAY:
                return OBJECT_TYPE;
            case Type.BYTE:
            case Type.SHORT:
                return Type.INT_TYPE;
            default:
                return type;
        }
    }

    public static void genThrow(@NotNull InstructionAdapter v, @NotNull String exception, @Nullable String message) {
        v.anew(Type.getObjectType(exception));
        v.dup();
        if (message != null) {
            v.aconst(message);
            v.invokespecial(exception, "<init>", "(Ljava/lang/String;)V", false);
        }
        else {
            v.invokespecial(exception, "<init>", "()V", false);
        }
        v.athrow();
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

    public static void genStringBuilderConstructor(InstructionAdapter v) {
        v.visitTypeInsn(NEW, "java/lang/StringBuilder");
        v.dup();
        v.invokespecial("java/lang/StringBuilder", "<init>", "()V", false);
    }

    public static void genInvokeAppendMethod(@NotNull InstructionAdapter v, @NotNull Type type, @Nullable KotlinType kotlinType) {
        genInvokeAppendMethod(v, type, kotlinType, null);
    }

    public static void genInvokeAppendMethod(
            @NotNull InstructionAdapter v,
            @NotNull Type type,
            @Nullable KotlinType kotlinType,
            @Nullable KotlinTypeMapper typeMapper
    ) {
        Type appendParameterType;

        CallableMethod specializedToString = getSpecializedToStringCallableMethodOrNull(kotlinType, typeMapper);
        if (specializedToString != null) {
            specializedToString.genInvokeInstruction(v);
            appendParameterType = AsmTypes.JAVA_STRING_TYPE;
        }
        else if (kotlinType != null && InlineClassesUtilsKt.isInlineClassType(kotlinType)) {
            appendParameterType = OBJECT_TYPE;
            SimpleType nullableAnyType = kotlinType.getConstructor().getBuiltIns().getNullableAnyType();
            StackValue.coerce(type, kotlinType, appendParameterType, nullableAnyType, v);
        }
        else {
            appendParameterType = stringBuilderAppendType(type);
        }

        v.invokevirtual("java/lang/StringBuilder", "append", "(" + appendParameterType.getDescriptor() + ")Ljava/lang/StringBuilder;", false);
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
        assert receiverTypeDescriptor instanceof ClassDescriptor && ((ClassDescriptor) receiverTypeDescriptor).isInline() :
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

    static void genHashCode(MethodVisitor mv, InstructionAdapter iv, Type type, JvmTarget jvmTarget) {
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
        else if (type.getSort() == Type.BOOLEAN) {
            Label end = new Label();
            iv.dup();
            iv.ifeq(end);
            iv.pop();
            iv.iconst(1);
            iv.mark(end);
        }
        else {
            if (JvmTarget.JVM_1_6 == jvmTarget) {
                if (type.getSort() == Type.LONG) {
                    genLongHashCode(mv, iv);
                }
                else if (type.getSort() == Type.DOUBLE) {
                    iv.invokestatic("java/lang/Double", "doubleToLongBits", "(D)J", false);
                    genLongHashCode(mv, iv);
                }
                else if (type.getSort() == Type.FLOAT) {
                    iv.invokestatic("java/lang/Float", "floatToIntBits", "(F)I", false);
                }
                else { // byte short char int
                    // do nothing
                }
            } else {
                HashCode.Companion.invokeHashCode(iv, type);
            }
        }
    }

    private static void genLongHashCode(MethodVisitor mv, InstructionAdapter iv) {
        iv.dup2();
        iv.iconst(32);
        iv.ushr(Type.LONG_TYPE);
        iv.xor(Type.LONG_TYPE);
        mv.visitInsn(L2I);
    }

    public static void genInvertBoolean(InstructionAdapter v) {
        v.iconst(1);
        v.xor(Type.INT_TYPE);
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
            genAreEqualCall(v);

            if (opToken == KtTokens.EXCLEQ || opToken == KtTokens.EXCLEQEQEQ) {
                genInvertBoolean(v);
            }
            return Unit.INSTANCE;
        });
    }

    public static void genAreEqualCall(InstructionAdapter v) {
        v.invokestatic(IntrinsicMethods.INTRINSICS_CLASS_NAME, "areEqual", "(Ljava/lang/Object;Ljava/lang/Object;)Z", false);
    }

    public static void genIEEE754EqualForNullableTypesCall(InstructionAdapter v, Type left, Type right) {
        v.invokestatic(IntrinsicMethods.INTRINSICS_CLASS_NAME, "areEqual", "(" + left.getDescriptor() + right.getDescriptor() + ")Z", false);
    }

    public static void numConst(int value, Type type, InstructionAdapter v) {
        if (type == Type.FLOAT_TYPE) {
            v.fconst(value);
        }
        else if (type == Type.DOUBLE_TYPE) {
            v.dconst(value);
        }
        else if (type == Type.LONG_TYPE) {
            v.lconst(value);
        }
        else if (type == Type.CHAR_TYPE || type == Type.BYTE_TYPE || type == Type.SHORT_TYPE || type == Type.INT_TYPE) {
            v.iconst(value);
        }
        else {
            throw new IllegalArgumentException("Primitive numeric type expected, got: " + type);
        }
    }

    public static void genIncrement(Type baseType, int myDelta, InstructionAdapter v) {
        Type operationType = numberFunctionOperandType(baseType);
        numConst(myDelta, operationType, v);
        v.add(operationType);
        StackValue.coerce(operationType, baseType, v);
    }

    public static void swap(InstructionAdapter v, Type stackTop, Type afterTop) {
        if (stackTop.getSize() == 1) {
            if (afterTop.getSize() == 1) {
                v.swap();
            }
            else {
                v.dupX2();
                v.pop();
            }
        }
        else {
            if (afterTop.getSize() == 1) {
                v.dup2X1();
            }
            else {
                v.dup2X2();
            }
            v.pop2();
        }
    }

    static void genNotNullAssertionsForParameters(
            @NotNull InstructionAdapter v,
            @NotNull GenerationState state,
            @NotNull FunctionDescriptor descriptor,
            @NotNull FrameMap frameMap
    ) {
        if (state.isParamAssertionsDisabled()) return;
        // currently when resuming a suspend function we pass default values instead of real arguments (i.e. nulls for references)
        if (descriptor.isSuspend()) return;

        if (getVisibilityAccessFlag(descriptor) == ACC_PRIVATE) {
            // Private method is not accessible from other classes, no assertions needed,
            // unless we have a private operator function, in which we should generate a parameter assertion for an extension receiver.

            // HACK: this provides "fail fast" behavior for operator functions.
            // Such functions can be invoked in operator conventions desugaring,
            // which is currently done on ad hoc basis in ExpressionCodegen.

            if (state.isReceiverAssertionsDisabled()) return;
            if (descriptor.isOperator()) {
                ReceiverParameterDescriptor receiverParameter = descriptor.getExtensionReceiverParameter();
                if (receiverParameter != null) {
                    String name = getNameForReceiverParameter(descriptor, state.getBindingContext(), state.getLanguageVersionSettings());
                    genParamAssertion(v, state.getTypeMapper(), frameMap, receiverParameter, name, descriptor);
                }
            }
            return;
        }

        ReceiverParameterDescriptor receiverParameter = descriptor.getExtensionReceiverParameter();
        if (receiverParameter != null) {
            String name = getNameForReceiverParameter(descriptor, state.getBindingContext(), state.getLanguageVersionSettings());
            genParamAssertion(v, state.getTypeMapper(), frameMap, receiverParameter, name, descriptor);
        }

        for (ValueParameterDescriptor parameter : descriptor.getValueParameters()) {
            genParamAssertion(v, state.getTypeMapper(), frameMap, parameter, parameter.getName().asString(), descriptor);
        }
    }

    private static void genParamAssertion(
            @NotNull InstructionAdapter v,
            @NotNull KotlinTypeMapper typeMapper,
            @NotNull FrameMap frameMap,
            @NotNull ParameterDescriptor parameter,
            @NotNull String name,
            @NotNull FunctionDescriptor containingDeclaration
    ) {
        KotlinType type = parameter.getType();
        if (isNullableType(type) || InlineClassesUtilsKt.isNullableUnderlyingType(type)) return;

        Type asmType = typeMapper.mapType(type);
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
            v.invokestatic(
                    IntrinsicMethods.INTRINSICS_CLASS_NAME, "checkParameterIsNotNull", "(Ljava/lang/Object;Ljava/lang/String;)V", false
            );
        }
    }

    @NotNull
    public static StackValue genNotNullAssertions(
            @NotNull GenerationState state,
            @NotNull StackValue stackValue,
            @Nullable RuntimeAssertionInfo runtimeAssertionInfo
    ) {
        if (state.isCallAssertionsDisabled()) return stackValue;
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
                    v.invokestatic(IntrinsicMethods.INTRINSICS_CLASS_NAME, "checkExpressionValueIsNotNull",
                                   "(Ljava/lang/Object;Ljava/lang/String;)V", false);
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

    public static void pushDefaultValueOnStack(@NotNull Type type, @NotNull InstructionAdapter v) {
        if (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY) {
            v.aconst(null);
        }
        else {
            pushDefaultPrimitiveValueOnStack(type, v);
        }
    }

    public static void pushDefaultPrimitiveValueOnStack(@NotNull Type type, @NotNull InstructionAdapter v) {
        if (type.getSort() == Type.FLOAT) {
            v.fconst(0);
        }
        else if (type.getSort() == Type.DOUBLE) {
            v.dconst(0);
        }
        else if (type.getSort() == Type.LONG) {
            v.lconst(0);
        }
        else {
            v.iconst(0);
        }
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

    public static Type comparisonOperandType(Type left, Type right) {
        if (left == Type.DOUBLE_TYPE || right == Type.DOUBLE_TYPE) return Type.DOUBLE_TYPE;
        if (left == Type.FLOAT_TYPE || right == Type.FLOAT_TYPE) return Type.FLOAT_TYPE;
        if (left == Type.LONG_TYPE || right == Type.LONG_TYPE) return Type.LONG_TYPE;
        if (left == Type.CHAR_TYPE || right == Type.CHAR_TYPE) return Type.CHAR_TYPE;
        return Type.INT_TYPE;
    }

    @NotNull
    public static Type numberFunctionOperandType(@NotNull Type expectedType) {
        if (expectedType == Type.SHORT_TYPE || expectedType == Type.BYTE_TYPE || expectedType == Type.CHAR_TYPE) {
            return Type.INT_TYPE;
        }
        return expectedType;
    }

    public static void pop(@NotNull MethodVisitor v, @NotNull Type type) {
        if (type.getSize() == 2) {
            v.visitInsn(Opcodes.POP2);
        }
        else {
            v.visitInsn(Opcodes.POP);
        }
    }

    public static void pop2(@NotNull MethodVisitor v, @NotNull Type type) {
        if (type.getSize() == 2) {
            v.visitInsn(Opcodes.POP2);
            v.visitInsn(Opcodes.POP2);
        }
        else {
            v.visitInsn(Opcodes.POP2);
        }
    }

    public static void dup(@NotNull InstructionAdapter v, @NotNull Type type) {
        dup(v, type.getSize());
    }

    private static void dup(@NotNull InstructionAdapter v, int size) {
        if (size == 2) {
            v.dup2();
        }
        else if (size == 1) {
            v.dup();
        }
        else {
            throw new UnsupportedOperationException();
        }
    }

    public static void dupx(@NotNull InstructionAdapter v, @NotNull Type type) {
        dupx(v, type.getSize());
    }

    private static void dupx(@NotNull InstructionAdapter v, int size) {
        if (size == 2) {
            v.dup2X2();
        }
        else if (size == 1) {
            v.dupX1();
        }
        else {
            throw new UnsupportedOperationException();
        }
    }

    public static void dup(@NotNull InstructionAdapter v, @NotNull Type topOfStack, @NotNull Type afterTop) {
        if (topOfStack.getSize() == 0 && afterTop.getSize() == 0) {
            return;
        }

        if (topOfStack.getSize() == 0) {
            dup(v, afterTop);
        }
        else if (afterTop.getSize() == 0) {
            dup(v, topOfStack);
        }
        else if (afterTop.getSize() == 1) {
            if (topOfStack.getSize() == 1) {
                dup(v, 2);
            }
            else {
                v.dup2X1();
                v.pop2();
                v.dupX2();
                v.dupX2();
                v.pop();
                v.dup2X1();
            }
        }
        else {
            //Note: it's possible to write dup3 and dup4
            throw new UnsupportedOperationException("Don't know how generate dup3/dup4 for: " + topOfStack + " and " + afterTop);
        }
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
        writeAnnotationData(av, JvmProtoBufUtil.writeData(message, stringTable), ArrayUtil.toStringArray(stringTable.getStrings()));
    }

    public static void writeAnnotationData(
            @NotNull AnnotationVisitor av, @NotNull String[] data, @NotNull String[] strings
    ) {
        AnnotationVisitor dataVisitor = av.visitArray(JvmAnnotationNames.METADATA_DATA_FIELD_NAME);
        for (String string : data) {
            dataVisitor.visit(null, string);
        }
        dataVisitor.visitEnd();

        AnnotationVisitor stringsVisitor = av.visitArray(JvmAnnotationNames.METADATA_STRINGS_FIELD_NAME);
        for (String string : strings) {
            stringsVisitor.visit(null, string);
        }
        stringsVisitor.visitEnd();
    }

    @NotNull
    public static Type asmTypeByFqNameWithoutInnerClasses(@NotNull FqName fqName) {
        return Type.getObjectType(internalNameByFqNameWithoutInnerClasses(fqName));
    }

    @NotNull
    public static Type asmTypeByClassId(@NotNull ClassId classId) {
        return Type.getObjectType(classId.asString().replace('.', '$'));
    }

    @NotNull
    public static String internalNameByFqNameWithoutInnerClasses(@NotNull FqName fqName) {
        return JvmClassName.byFqNameWithoutInnerClasses(fqName).getInternalName();
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
            v.getstatic(boxType(type).getInternalName(), "TYPE", "Ljava/lang/Class;");
        }
        else {
            v.aconst(type);
        }
    }

    public static void wrapJavaClassIntoKClass(@NotNull InstructionAdapter v) {
        v.invokestatic(REFLECTION, "getOrCreateKotlinClass", Type.getMethodDescriptor(K_CLASS_TYPE, getType(Class.class)), false);
    }

    public static void wrapJavaClassesIntoKClasses(@NotNull InstructionAdapter v) {
        v.invokestatic(REFLECTION, "getOrCreateKotlinClasses", Type.getMethodDescriptor(K_CLASS_ARRAY_TYPE, getType(Class[].class)), false);
    }

    public static int getReceiverIndex(@NotNull CodegenContext context, @NotNull CallableMemberDescriptor descriptor) {
        OwnerKind kind = context.getContextKind();
        //Trait always should have this descriptor
        return kind != OwnerKind.DEFAULT_IMPLS && isStaticMethod(kind, descriptor) ? 0 : 1;
    }
}
