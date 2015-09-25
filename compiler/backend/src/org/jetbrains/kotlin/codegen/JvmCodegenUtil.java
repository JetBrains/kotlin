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
import kotlin.KotlinPackage;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.codegen.binding.CalculatedClosure;
import org.jetbrains.kotlin.codegen.context.CodegenContext;
import org.jetbrains.kotlin.codegen.context.MethodContext;
import org.jetbrains.kotlin.codegen.context.PackageContext;
import org.jetbrains.kotlin.codegen.context.RootContext;
import org.jetbrains.kotlin.codegen.state.JetTypeMapper;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.load.java.JvmAbi;
import org.jetbrains.kotlin.load.java.JvmAnnotationNames;
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor;
import org.jetbrains.kotlin.load.kotlin.ModuleMapping;
import org.jetbrains.kotlin.load.kotlin.ModuleVisibilityUtilsKt;
import org.jetbrains.kotlin.load.kotlin.incremental.IncrementalPackageFragmentProvider;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.psi.JetFunction;
import org.jetbrains.kotlin.psi.codeFragmentUtil.CodeFragmentUtilPackage;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.inline.InlineUtil;
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedCallableMemberDescriptor;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.org.objectweb.asm.AnnotationVisitor;

import java.io.File;

import static org.jetbrains.kotlin.descriptors.Modality.ABSTRACT;
import static org.jetbrains.kotlin.descriptors.Modality.FINAL;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.isTrait;

public class JvmCodegenUtil {

    private JvmCodegenUtil() {
    }

    public static boolean isInterface(DeclarationDescriptor descriptor) {
        if (descriptor instanceof ClassDescriptor) {
            ClassKind kind = ((ClassDescriptor) descriptor).getKind();
            return kind == ClassKind.INTERFACE || kind == ClassKind.ANNOTATION_CLASS;
        }
        return false;
    }

    public static boolean isInterface(JetType type) {
        return isInterface(type.getConstructor().getDeclarationDescriptor());
    }

    public static boolean isConst(@NotNull CalculatedClosure closure) {
        return closure.getCaptureThis() == null && closure.getCaptureReceiverType() == null && closure.getCaptureVariables().isEmpty();
    }

    private static boolean isCallInsideSameClassAsDeclared(@NotNull CallableMemberDescriptor descriptor, @NotNull CodegenContext context) {
        boolean isFakeOverride = descriptor.getKind() == CallableMemberDescriptor.Kind.FAKE_OVERRIDE;
        boolean isDelegate = descriptor.getKind() == CallableMemberDescriptor.Kind.DELEGATION;

        DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration().getOriginal();

        return !isFakeOverride && !isDelegate &&
               (((context.hasThisDescriptor() && containingDeclaration == context.getThisDescriptor()) ||
                 (context.getParentContext() instanceof PackageContext
                  && isSamePackageInSameModule(context.getParentContext().getContextDescriptor(), containingDeclaration)))
                && context.getContextKind() != OwnerKind.TRAIT_IMPL);
    }

    private static boolean isSamePackageInSameModule(
            @NotNull DeclarationDescriptor callerOwner,
            @NotNull DeclarationDescriptor calleeOwner
    ) {
        if (callerOwner instanceof PackageFragmentDescriptor && calleeOwner instanceof PackageFragmentDescriptor) {
            PackageFragmentDescriptor callerFragment = (PackageFragmentDescriptor) callerOwner;
            PackageFragmentDescriptor calleeFragment = (PackageFragmentDescriptor) calleeOwner;

            // backing field should be used directly within same module of same package
            if (callerFragment == calleeFragment) {
                return true;
            }
            return callerFragment.getFqName().equals(calleeFragment.getFqName())
                   && calleeFragment instanceof IncrementalPackageFragmentProvider.IncrementalPackageFragment;
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
        return KotlinPackage.any(classDescriptor.getDefaultType().getMemberScope().getAllDescriptors(),
                                 new Function1<DeclarationDescriptor, Boolean>() {
                                     @Override
                                     public Boolean invoke(DeclarationDescriptor descriptor) {
                                         return descriptor instanceof CallableMemberDescriptor &&
                                                ((CallableMemberDescriptor) descriptor).getModality() == ABSTRACT;
                                     }
                                 }
        );
    }

    public static boolean couldUseDirectAccessToProperty(
            @NotNull PropertyDescriptor property,
            boolean forGetter,
            boolean isDelegated,
            @NotNull MethodContext context
    ) {
        if (JetTypeMapper.isAccessor(property)) return false;

        // Inline functions can't use direct access because a field may not be visible at the call site
        if (context.isInlineFunction() &&
            (!Visibilities.isPrivate(property.getVisibility()) || DescriptorUtils.isTopLevelDeclaration(property))) {
            return false;
        }

        // Only properties of the same class can be directly accessed, except when we are evaluating expressions in the debugger
        if (!isCallInsideSameClassAsDeclared(property, context) && !isDebuggerContext(context)) return false;

        // Delegated and extension properties have no backing fields
        if (isDelegated || property.getExtensionReceiverParameter() != null) return false;

        // Companion object properties cannot be accessed directly because their backing fields are stored in the containing class
        if (DescriptorUtils.isCompanionObject(property.getContainingDeclaration())) return false;

        PropertyAccessorDescriptor accessor = forGetter ? property.getGetter() : property.getSetter();

        // If there's no accessor declared we can use direct access
        if (accessor == null) return true;

        // If the accessor is non-default (i.e. it has some code) we should call that accessor and not use direct access
        if (accessor.hasBody()) return false;

        // If the accessor is private or final, it can't be overridden in the subclass and thus we can use direct access
        return Visibilities.isPrivate(property.getVisibility()) || accessor.getModality() == FINAL;
    }

    private static boolean isDebuggerContext(@NotNull MethodContext context) {
        JetFile file = DescriptorToSourceUtils.getContainingFile(context.getContextDescriptor());
        return file != null && CodeFragmentUtilPackage.getSuppressDiagnosticsInDebugMode(file);
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
               InlineUtil.isInlinedArgument((JetFunction) declaration, bindingContext, false);
    }

    public static boolean shouldUseJavaClassForClassLiteral(@NotNull ClassifierDescriptor descriptor) {
        ModuleDescriptor module = DescriptorUtils.getContainingModule(descriptor);
        return descriptor instanceof JavaClassDescriptor ||
               module == module.getBuiltIns().getBuiltInsModule() ||
               DescriptorUtils.isAnnotationClass(descriptor);
    }

    @NotNull
    public static String getModuleName(ModuleDescriptor module) {
        return KotlinPackage.removeSurrounding(module.getName().asString(), "<", ">");
    }

    @NotNull
    public static String getMappingFileName(@NotNull String moduleName) {
        return "META-INF/" + moduleName + "." + ModuleMapping.MAPPING_FILE_EXT;
    }

    public static void writeAbiVersion(@NotNull AnnotationVisitor av) {
        av.visit(JvmAnnotationNames.VERSION_FIELD_NAME, JvmAbi.VERSION.toArray());

        // TODO: drop after some time
        av.visit(JvmAnnotationNames.OLD_ABI_VERSION_FIELD_NAME, JvmAbi.VERSION.getMinor());
    }
}
