/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.codegen.AnnotationCodegen;
import org.jetbrains.kotlin.codegen.AsmUtil;
import org.jetbrains.kotlin.codegen.CodegenUtilKt;
import org.jetbrains.kotlin.codegen.OwnerKind;
import org.jetbrains.kotlin.config.JvmDefaultMode;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.load.java.DescriptorsJvmAbiUtil;
import org.jetbrains.kotlin.renderer.DescriptorRenderer;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.InlineClassDescriptorResolver;
import org.jetbrains.kotlin.resolve.InlineClassesUtilsKt;
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver;
import org.jetbrains.kotlin.resolve.inline.InlineUtil;
import org.jetbrains.kotlin.resolve.jvm.annotations.JvmAnnotationUtilKt;
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor;
import org.jetbrains.org.objectweb.asm.Opcodes;

import static org.jetbrains.kotlin.codegen.AsmUtil.NO_FLAG_PACKAGE_PRIVATE;
import static org.jetbrains.kotlin.codegen.CodegenUtilKt.isToArrayFromCollection;
import static org.jetbrains.kotlin.codegen.JvmCodegenUtil.isJvmInterface;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.*;
import static org.jetbrains.kotlin.resolve.inline.InlineOnlyKt.isInlineOnlyPrivateInBytecode;
import static org.jetbrains.kotlin.resolve.inline.InlineOnlyKt.isInlineWithReified;
import static org.jetbrains.kotlin.resolve.jvm.annotations.JvmAnnotationUtilKt.hasJvmSyntheticAnnotation;
import static org.jetbrains.org.objectweb.asm.Opcodes.*;

public class VisibilityUtil {
    private VisibilityUtil() {}

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

    private static int getVisibilityAccessFlag(@NotNull MemberDescriptor descriptor, @Nullable OwnerKind kind) {
        Integer specialCase = specialCaseVisibility(descriptor, kind);
        if (specialCase != null) {
            return specialCase;
        }
        DescriptorVisibility visibility = descriptor.getVisibility();
        Integer defaultMapping = AsmUtil.getVisibilityAccessFlag(visibility.getDelegate());
        if (defaultMapping == null) {
            throw new IllegalStateException(
                    visibility + " is not a valid visibility in backend for " + DescriptorRenderer.DEBUG_TEXT.render(descriptor)
            );
        }
        return defaultMapping;
    }

    private static int getVisibilityAccessFlagForAnonymous(@NotNull ClassDescriptor descriptor) {
        return InlineUtil.isInlineOrContainingInline(descriptor.getContainingDeclaration()) ? ACC_PUBLIC : NO_FLAG_PACKAGE_PRIVATE;
    }

    private static int getDeprecatedAccessFlag(@NotNull MemberDescriptor descriptor) {
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

        if (isNonIntrinsicPrivateCompanionObjectInInterface(memberDescriptor)) {
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
            return getVisibilityAccessFlag(((SyntheticJavaPropertyDescriptor) memberDescriptor).getGetMethod(), null);
        }
        if (memberDescriptor instanceof PropertyAccessorDescriptor) {
            PropertyDescriptor property = ((PropertyAccessorDescriptor) memberDescriptor).getCorrespondingProperty();
            if (property instanceof SyntheticJavaPropertyDescriptor) {
                FunctionDescriptor method = memberDescriptor == property.getGetter()
                                            ? ((SyntheticJavaPropertyDescriptor) property).getGetMethod()
                                            : ((SyntheticJavaPropertyDescriptor) property).getSetMethod();
                assert method != null : "No get/set method in SyntheticJavaPropertyDescriptor: " + property;
                return getVisibilityAccessFlag(method, null);
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

    private static boolean isNonIntrinsicPrivateCompanionObjectInInterface(@NotNull DeclarationDescriptorWithVisibility companionObject) {
        return isCompanionObject(companionObject) &&
               isJvmInterface(companionObject.getContainingDeclaration()) &&
               !DescriptorsJvmAbiUtil.isMappedIntrinsicCompanionObject((ClassDescriptor) companionObject) &&
               DescriptorVisibilities.isPrivate(companionObject.getVisibility());
    }
}
