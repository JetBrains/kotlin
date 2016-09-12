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

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function1;
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
import org.jetbrains.kotlin.load.java.descriptors.JavaPropertyDescriptor;
import org.jetbrains.kotlin.load.kotlin.*;
import org.jetbrains.kotlin.psi.Call;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtFunction;
import org.jetbrains.kotlin.psi.codeFragmentUtil.CodeFragmentUtilKt;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.inline.InlineUtil;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;
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
import static org.jetbrains.kotlin.resolve.jvm.annotations.AnnotationUtilKt.hasJvmFieldAnnotation;

public class JvmCodegenUtil {

    private JvmCodegenUtil() {
    }

    public static boolean isAnnotationOrJvm6Interface(@NotNull DeclarationDescriptor descriptor, @NotNull GenerationState state) {
        if (!isJvmInterface(descriptor)) {
            return false;
        }
        if (ANNOTATION_CLASS == ((ClassDescriptor) descriptor).getKind()) return true;

        if (descriptor instanceof DeserializedClassDescriptor) {
            SourceElement source = ((DeserializedClassDescriptor) descriptor).getSource();
            if (source instanceof KotlinJvmBinarySourceElement) {
                KotlinJvmBinaryClass binaryClass = ((KotlinJvmBinarySourceElement) source).getBinaryClass();
                assert binaryClass instanceof FileBasedKotlinClass :
                        "KotlinJvmBinaryClass should be subclass of FileBasedKotlinClass, but " + binaryClass;
                return ((FileBasedKotlinClass) binaryClass).getClassVersion() == Opcodes.V1_6;
            }
        }
        return !state.isJvm8Target();
    }

    public static boolean isJvm8Interface(@NotNull DeclarationDescriptor descriptor, @NotNull GenerationState state) {
        return DescriptorUtils.isInterface(descriptor) && !isAnnotationOrJvm6Interface(descriptor, state);
    }

    public static boolean isJvm8InterfaceMember(@NotNull CallableMemberDescriptor descriptor, @NotNull GenerationState state) {
        DeclarationDescriptor declaration = descriptor.getContainingDeclaration();
        return isJvm8Interface(declaration, state);
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
                    !closure.isCoroutine();
    }

    private static boolean isCallInsideSameClassAsDeclared(@NotNull CallableMemberDescriptor descriptor, @NotNull CodegenContext context) {
        boolean isFakeOverride = descriptor.getKind() == CallableMemberDescriptor.Kind.FAKE_OVERRIDE;
        boolean isDelegate = descriptor.getKind() == CallableMemberDescriptor.Kind.DELEGATION;

        DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration().getOriginal();

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
        return CollectionsKt.any(DescriptorUtils.getAllDescriptors(classDescriptor.getDefaultType().getMemberScope()),
                                 new Function1<DeclarationDescriptor, Boolean>() {
                                     @Override
                                     public Boolean invoke(DeclarationDescriptor descriptor) {
                                         return descriptor instanceof CallableMemberDescriptor &&
                                                ((CallableMemberDescriptor) descriptor).getModality() == ABSTRACT;
                                     }
                                 }
        );
    }

    public static boolean isConstOrHasJvmFieldAnnotation(@NotNull PropertyDescriptor propertyDescriptor) {
        return propertyDescriptor.isConst() || hasJvmFieldAnnotation(propertyDescriptor);
    }

    public static boolean couldUseDirectAccessToProperty(
            @NotNull PropertyDescriptor property,
            boolean forGetter,
            boolean isDelegated,
            @NotNull MethodContext contextBeforeInline
    ) {
        if (KotlinTypeMapper.isAccessor(property)) return false;

        CodegenContext context = contextBeforeInline.getFirstCrossInlineOrNonInlineContext();
        // Inline functions can't use direct access because a field may not be visible at the call site
        if (context.isInlineMethodContext()) {
            return false;
        }

        if (!isCallInsideSameClassAsDeclared(property, context)) {
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

        // Companion object properties cannot be accessed directly because their backing fields are stored in the containing class
        if (DescriptorUtils.isCompanionObject(property.getContainingDeclaration())) return false;

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
        return descriptor instanceof PropertyAccessorDescriptor
               ? ((PropertyAccessorDescriptor) descriptor).getCorrespondingProperty()
               : descriptor;
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
        if (!(descriptor instanceof JavaPropertyDescriptor)) return false;
        return descriptor.isConst();
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
}
