/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import kotlin.collections.CollectionsKt;
import kotlin.text.StringsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.codegen.binding.CalculatedClosure;
import org.jetbrains.kotlin.codegen.context.CodegenContext;
import org.jetbrains.kotlin.codegen.context.FacadePartWithSourceFile;
import org.jetbrains.kotlin.codegen.context.MethodContext;
import org.jetbrains.kotlin.codegen.context.RootContext;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor;
import org.jetbrains.kotlin.load.java.JvmAbi;
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor;
import org.jetbrains.kotlin.load.java.descriptors.JavaPropertyDescriptor;
import org.jetbrains.kotlin.load.kotlin.*;
import org.jetbrains.kotlin.psi.Call;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtFunction;
import org.jetbrains.kotlin.psi.codeFragmentUtil.CodeFragmentUtilKt;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.inline.InlineUtil;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.kotlin.resolve.scopes.receivers.TransientReceiver;
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedCallableMemberDescriptor;
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.org.objectweb.asm.Opcodes;

import java.io.File;

import static org.jetbrains.kotlin.descriptors.ClassKind.ANNOTATION_CLASS;
import static org.jetbrains.kotlin.descriptors.ClassKind.INTERFACE;
import static org.jetbrains.kotlin.descriptors.Modality.ABSTRACT;
import static org.jetbrains.kotlin.descriptors.Modality.FINAL;
import static org.jetbrains.kotlin.resolve.BindingContext.DELEGATED_PROPERTY_CALL;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.isCompanionObject;
import static org.jetbrains.kotlin.resolve.jvm.annotations.AnnotationUtilKt.hasJvmFieldAnnotation;

public class JvmCodegenUtil {

    private JvmCodegenUtil() {
    }

    public static boolean isInterfaceWithoutDefaults(@NotNull DeclarationDescriptor descriptor, @NotNull GenerationState state) {
        return isInterfaceWithoutDefaults(descriptor, state.isJvm8Target(), state.isJvm8TargetWithDefaults());
    }

    private static boolean isInterfaceWithoutDefaults(@NotNull DeclarationDescriptor descriptor, boolean isJvm8Target, boolean isJvm8TargetWithDefaults) {
        if (!DescriptorUtils.isInterface(descriptor)) return false;

        if (descriptor instanceof DeserializedClassDescriptor) {
            SourceElement source = ((DeserializedClassDescriptor) descriptor).getSource();
            if (source instanceof KotlinJvmBinarySourceElement) {
                KotlinJvmBinaryClass binaryClass = ((KotlinJvmBinarySourceElement) source).getBinaryClass();
                assert binaryClass instanceof FileBasedKotlinClass :
                        "KotlinJvmBinaryClass should be subclass of FileBasedKotlinClass, but " + binaryClass;
                /*TODO need add some flags to compiled code*/
                return true || ((FileBasedKotlinClass) binaryClass).getClassVersion() == Opcodes.V1_6;
            }
        }
        return !isJvm8TargetWithDefaults;
    }

    public static boolean isJvm8InterfaceWithDefaults(@NotNull DeclarationDescriptor descriptor, @NotNull GenerationState state) {
        return isJvm8InterfaceWithDefaults(descriptor, state.isJvm8Target(), state.isJvm8TargetWithDefaults());
    }

    public static boolean isJvm8InterfaceWithDefaults(@NotNull DeclarationDescriptor descriptor, boolean isJvm8Target, boolean isJvm8TargetWithDefaults) {
        return DescriptorUtils.isInterface(descriptor) && !isInterfaceWithoutDefaults(descriptor, isJvm8Target, isJvm8TargetWithDefaults);
    }

    public static boolean isJvm8InterfaceWithDefaultsMember(@NotNull CallableMemberDescriptor descriptor, @NotNull GenerationState state) {
        DeclarationDescriptor declaration = descriptor.getContainingDeclaration();
        return isJvm8InterfaceWithDefaults(declaration, state);
    }

    public static boolean isNonDefaultInterfaceMember(@NotNull CallableMemberDescriptor descriptor, @NotNull GenerationState state) {
        if (!isJvmInterface(descriptor.getContainingDeclaration())) {
            return false;
        }
        if (descriptor instanceof JavaCallableMemberDescriptor) {
            return descriptor.getModality() == Modality.ABSTRACT;
        }

        return !isJvm8InterfaceWithDefaultsMember(descriptor, state);
    }

    public static boolean isJvmInterface(DeclarationDescriptor descriptor) {
        if (descriptor instanceof ClassDescriptor) {
            ClassKind kind = ((ClassDescriptor) descriptor).getKind();
            return kind == INTERFACE || kind == ANNOTATION_CLASS;
        }
        return false;
    }

    public static boolean isJvmInterface(KotlinType type) {
        return isJvmInterface(type.getConstructor().getDeclarationDescriptor());
    }

    public static boolean isConst(@NotNull CalculatedClosure closure) {
        return closure.getCaptureThis() == null &&
                    closure.getCaptureReceiverType() == null &&
                    closure.getCaptureVariables().isEmpty() &&
                    !closure.isSuspend();
    }

    private static boolean isCallInsideSameClassAsFieldRepresentingProperty(
            @NotNull PropertyDescriptor descriptor,
            @NotNull CodegenContext context
    ) {
        boolean isFakeOverride = descriptor.getKind() == CallableMemberDescriptor.Kind.FAKE_OVERRIDE;
        boolean isDelegate = descriptor.getKind() == CallableMemberDescriptor.Kind.DELEGATION;

        DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration().getOriginal();
        if (JvmAbi.isPropertyWithBackingFieldInOuterClass(descriptor)) {
            // For property with backed field, check if the access is done in the same class containing the backed field and
            // not the class that declared the field.
            containingDeclaration = containingDeclaration.getContainingDeclaration();
        }

        return !isFakeOverride && !isDelegate &&
               (((context.hasThisDescriptor() && containingDeclaration == context.getThisDescriptor()) ||
                 ((context.getParentContext() instanceof FacadePartWithSourceFile)
                  && isWithinSameFile(((FacadePartWithSourceFile) context.getParentContext()).getSourceFile(), descriptor)))
                && context.getContextKind() != OwnerKind.DEFAULT_IMPLS);
    }

    private static boolean isWithinSameFile(
            @Nullable KtFile callerFile,
            @NotNull CallableMemberDescriptor descriptor
    ) {
        DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration().getOriginal();
        if (containingDeclaration instanceof PackageFragmentDescriptor) {
            PsiElement calleeElement = DescriptorToSourceUtils.descriptorToDeclaration(descriptor);
            PsiFile calleeFile = calleeElement != null ? calleeElement.getContainingFile() : null;
            return callerFile != null && callerFile != SourceFile.NO_SOURCE_FILE && calleeFile == callerFile;

        }
        return false;
    }

    public static boolean isCallInsideSameModuleAsDeclared(
            @NotNull CallableMemberDescriptor declarationDescriptor,
            @NotNull CodegenContext context,
            @Nullable File outDirectory
    ) {
        if (context instanceof RootContext) {
            return true;
        }
        DeclarationDescriptor contextDescriptor = context.getContextDescriptor();

        CallableMemberDescriptor directMember = getDirectMember(declarationDescriptor);
        if (directMember instanceof DeserializedCallableMemberDescriptor) {
            return ModuleVisibilityUtilsKt.isContainedByCompiledPartOfOurModule(directMember, outDirectory);
        }
        else {
            return DescriptorUtils.areInSameModule(directMember, contextDescriptor);
        }
    }

    public static boolean hasAbstractMembers(@NotNull ClassDescriptor classDescriptor) {
        return CollectionsKt.any(
                DescriptorUtils.getAllDescriptors(classDescriptor.getDefaultType().getMemberScope()),
                descriptor -> descriptor instanceof CallableMemberDescriptor &&
                              ((CallableMemberDescriptor) descriptor).getModality() == ABSTRACT
        );
    }

    public static boolean isConstOrHasJvmFieldAnnotation(@NotNull PropertyDescriptor propertyDescriptor) {
        return propertyDescriptor.isConst() || hasJvmFieldAnnotation(propertyDescriptor);
    }

    public static boolean couldUseDirectAccessToProperty(
            @NotNull PropertyDescriptor property,
            boolean forGetter,
            boolean isDelegated,
            @NotNull MethodContext contextBeforeInline,
            boolean shouldInlineConstVals
    ) {
        if (shouldInlineConstVals && property.isConst()) return true;

        if (KotlinTypeMapper.isAccessor(property)) return false;

        CodegenContext context = contextBeforeInline.getFirstCrossInlineOrNonInlineContext();
        // Inline functions or inline classes can't use direct access because a field may not be visible at the call site
        if (context.isInlineMethodContext() || (context.getEnclosingClass() != null && context.getEnclosingClass().isInline())) {
            return false;
        }

        if (!isCallInsideSameClassAsFieldRepresentingProperty(property, context)) {
            if (!isDebuggerContext(context)) {
                // Unless we are evaluating expression in debugger context, only properties of the same class can be directly accessed
                return false;
            }
            else {
                // In debugger we want to access through accessors if they are generated

                // Non default accessors must always be generated
                for (PropertyAccessorDescriptor accessorDescriptor : property.getAccessors()) {
                    if (!accessorDescriptor.isDefault()) {
                        if (forGetter == accessorDescriptor instanceof PropertyGetterDescriptor) {
                            return false;
                        }
                    }
                }

                // If property overrides something, accessors must be generated too
                if (!property.getOverriddenDescriptors().isEmpty()) return false;
            }
        }

        // Delegated and extension properties have no backing fields
        if (isDelegated || property.getExtensionReceiverParameter() != null) return false;

        PropertyAccessorDescriptor accessor = forGetter ? property.getGetter() : property.getSetter();

        // If there's no accessor declared we can use direct access
        if (accessor == null) return true;

        // If the accessor is non-default (i.e. it has some code) we should call that accessor and not use direct access
        if (DescriptorPsiUtilsKt.hasBody(accessor)) return false;

        // If the accessor is private or final, it can't be overridden in the subclass and thus we can use direct access
        return Visibilities.isPrivate(property.getVisibility()) || accessor.getModality() == FINAL;
    }

    private static boolean isDebuggerContext(@NotNull CodegenContext context) {
        KtFile file = DescriptorToSourceUtils.getContainingFile(context.getContextDescriptor());
        return file != null && CodeFragmentUtilKt.getSuppressDiagnosticsInDebugMode(file);
    }

    @Nullable
    public static ClassDescriptor getDispatchReceiverParameterForConstructorCall(
            @NotNull ConstructorDescriptor descriptor,
            @Nullable CalculatedClosure closure
    ) {
        //for compilation against sources
        if (closure != null) {
            return closure.getCaptureThis();
        }

        //for compilation against binaries
        //TODO: It's best to use this code also for compilation against sources
        // but sometimes structures that have dispatchReceiver (bug?) mapped to static classes
        ReceiverParameterDescriptor dispatchReceiver = descriptor.getDispatchReceiverParameter();
        if (dispatchReceiver != null) {
            ClassDescriptor expectedThisClass = (ClassDescriptor) dispatchReceiver.getContainingDeclaration();
            if (!expectedThisClass.getKind().isSingleton()) {
                return expectedThisClass;
            }
        }

        return null;
    }

    @NotNull
    public static CallableMemberDescriptor getDirectMember(@NotNull CallableMemberDescriptor descriptor) {
        return DescriptorUtils.getDirectMember(descriptor);
    }

    public static boolean isArgumentWhichWillBeInlined(@NotNull BindingContext bindingContext, @NotNull DeclarationDescriptor descriptor) {
        PsiElement declaration = DescriptorToSourceUtils.descriptorToDeclaration(descriptor);
        return InlineUtil.canBeInlineArgument(declaration) &&
               InlineUtil.isInlinedArgument((KtFunction) declaration, bindingContext, false);
    }

    @NotNull
    public static String getModuleName(ModuleDescriptor module) {
        return StringsKt.removeSurrounding(module.getName().asString(), "<", ">");
    }

    @NotNull
    public static String getMappingFileName(@NotNull String moduleName) {
        return "META-INF/" + moduleName + "." + ModuleMapping.MAPPING_FILE_EXT;
    }

    public static boolean isInlinedJavaConstProperty(VariableDescriptor descriptor) {
        return descriptor instanceof JavaPropertyDescriptor && descriptor.isConst();
    }

    @Nullable
    public static KotlinType getPropertyDelegateType(
            @NotNull VariableDescriptorWithAccessors descriptor,
            @NotNull BindingContext bindingContext
    ) {
        VariableAccessorDescriptor getter = descriptor.getGetter();
        if (getter != null) {
            Call call = bindingContext.get(DELEGATED_PROPERTY_CALL, getter);
            if (call != null) {
                assert call.getExplicitReceiver() != null : "No explicit receiver for call:" + call;
                return ((ReceiverValue) call.getExplicitReceiver()).getType();
            }
        }
        return null;
    }

    public static boolean isDelegatedLocalVariable(@NotNull DeclarationDescriptor descriptor) {
        return descriptor instanceof LocalVariableDescriptor && ((LocalVariableDescriptor) descriptor).isDelegated();
    }

    @Nullable
    public static ReceiverValue getBoundCallableReferenceReceiver(@NotNull ResolvedCall<?> resolvedCall) {
        CallableDescriptor descriptor = resolvedCall.getResultingDescriptor();
        if (descriptor.getExtensionReceiverParameter() == null && descriptor.getDispatchReceiverParameter() == null) return null;

        ReceiverValue dispatchReceiver = resolvedCall.getDispatchReceiver();
        ReceiverValue extensionReceiver = resolvedCall.getExtensionReceiver();
        assert dispatchReceiver == null || extensionReceiver == null : "Cannot generate reference with both receivers: " + descriptor;
        ReceiverValue receiver = dispatchReceiver != null ? dispatchReceiver : extensionReceiver;

        if (receiver instanceof TransientReceiver) return null;

        return receiver;
    }

    public static boolean isCompanionObjectInInterfaceNotIntrinsic(@NotNull DeclarationDescriptor companionObject) {
        return isCompanionObject(companionObject) &&
               isJvmInterface(companionObject.getContainingDeclaration()) &&
               !JvmAbi.isMappedIntrinsicCompanionObject((ClassDescriptor) companionObject);
    }

    public static boolean hasBackingField(
            @NotNull PropertyDescriptor descriptor, @NotNull OwnerKind kind, @NotNull BindingContext bindingContext
    ) {
        return !isJvmInterface(descriptor.getContainingDeclaration()) &&
               kind != OwnerKind.DEFAULT_IMPLS &&
               !Boolean.FALSE.equals(bindingContext.get(BindingContext.BACKING_FIELD_REQUIRED, descriptor));
    }
}
