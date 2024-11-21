/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen;

import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.codegen.intrinsics.HashCode;
import org.jetbrains.kotlin.codegen.intrinsics.IntrinsicMethods;
import org.jetbrains.kotlin.config.JvmDefaultMode;
import org.jetbrains.kotlin.config.JvmTarget;
import org.jetbrains.kotlin.config.LanguageFeature;
import org.jetbrains.kotlin.config.LanguageVersionSettings;
import org.jetbrains.kotlin.descriptors.*;
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
import org.jetbrains.kotlin.resolve.jvm.annotations.JvmAnnotationUtilKt;
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor;
import org.jetbrains.org.objectweb.asm.AnnotationVisitor;
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.Opcodes;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;

import static org.jetbrains.kotlin.codegen.AsmUtil.*;
import static org.jetbrains.kotlin.codegen.CodegenUtilKt.isToArrayFromCollection;
import static org.jetbrains.kotlin.codegen.JvmCodegenUtil.isJvmInterface;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.isAnonymousObject;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.isEnumEntry;
import static org.jetbrains.kotlin.resolve.inline.InlineOnlyKt.isInlineOnlyPrivateInBytecode;
import static org.jetbrains.kotlin.resolve.inline.InlineOnlyKt.isInlineWithReified;
import static org.jetbrains.kotlin.resolve.jvm.annotations.JvmAnnotationUtilKt.hasJvmSyntheticAnnotation;
import static org.jetbrains.org.objectweb.asm.Opcodes.*;

public class DescriptorAsmUtil {
    private DescriptorAsmUtil() {
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

    private static boolean isAbstractMethod(FunctionDescriptor functionDescriptor, JvmDefaultMode jvmDefaultMode) {
        return (functionDescriptor.getModality() == Modality.ABSTRACT ||
                (isJvmInterface(functionDescriptor.getContainingDeclaration()) && !JvmAnnotationUtilKt
                        .isCompiledToJvmDefault(functionDescriptor, jvmDefaultMode))) &&
               !CodegenUtilKt.isJvmStaticInObjectOrClassOrInterface(functionDescriptor);
    }

    public static int getMethodAsmFlags(
            FunctionDescriptor functionDescriptor,
            DeprecationResolver deprecationResolver,
            JvmDefaultMode jvmDefaultMode
    ) {
        int flags = getCommonCallableFlags(functionDescriptor, deprecationResolver);

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

        if (CodegenUtilKt.isJvmStaticInObjectOrClassOrInterface(functionDescriptor)) {
            flags |= ACC_STATIC;
        }

        if (isAbstractMethod(functionDescriptor, jvmDefaultMode)) {
            flags |= ACC_ABSTRACT;
        }

        if (hasJvmSyntheticAnnotation(functionDescriptor) ||
            isInlineClassWrapperConstructor(functionDescriptor) ||
            InlineClassDescriptorResolver.isSynthesizedBoxMethod(functionDescriptor) ||
            InlineClassDescriptorResolver.isSynthesizedUnboxMethod(functionDescriptor)
        ) {
            flags |= ACC_SYNTHETIC;
        }

        return flags;
    }

    private static boolean isInlineClassWrapperConstructor(@NotNull FunctionDescriptor functionDescriptor) {
        if (!(functionDescriptor instanceof ConstructorDescriptor)) return false;
        ClassDescriptor classDescriptor = ((ConstructorDescriptor) functionDescriptor).getConstructedClass();
        return InlineClassesUtilsKt.isInlineClass(classDescriptor);
    }

    private static int getCommonCallableFlags(
            FunctionDescriptor functionDescriptor,
            @NotNull DeprecationResolver deprecationResolver
    ) {
        int flags = getVisibilityAccessFlag(functionDescriptor, OwnerKind.IMPLEMENTATION);
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

    private static int getVisibilityAccessFlagForAnonymous(@NotNull ClassDescriptor descriptor) {
        return InlineUtil.isInlineOrContainingInline(descriptor.getContainingDeclaration()) ? ACC_PUBLIC : NO_FLAG_PACKAGE_PRIVATE;
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
            kind != null && isInlineClassWrapperConstructor((FunctionDescriptor) memberDescriptor)) {
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

        if (memberDescriptor instanceof ConstructorDescriptor && isEnumEntry(containingDeclaration)) {
            return NO_FLAG_PACKAGE_PRIVATE;
        }

        return null;
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

    public static void genAreEqualCall(InstructionAdapter v) {
        v.invokestatic(IntrinsicMethods.INTRINSICS_CLASS_NAME, "areEqual", "(Ljava/lang/Object;Ljava/lang/Object;)Z", false);
    }

    public static void genIncrement(Type baseType, int myDelta, InstructionAdapter v) {
        Type operationType = numberFunctionOperandType(baseType);
        numConst(myDelta, operationType, v);
        v.add(operationType);
        StackValue.coerce(operationType, baseType, v);
    }

    public static void writeAnnotationData(
            @NotNull AnnotationVisitor av, @NotNull MessageLite message, @NotNull JvmStringTable stringTable
    ) {
        AsmUtil.writeAnnotationData(av, JvmProtoBufUtil.writeData(message, stringTable), ArrayUtil.toStringArray(stringTable.getStrings()));
    }
}
