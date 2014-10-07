/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.codegen;

import com.intellij.openapi.vfs.StandardFileSystems;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import kotlin.Function1;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.codegen.binding.CalculatedClosure;
import org.jetbrains.jet.codegen.context.CodegenContext;
import org.jetbrains.jet.codegen.context.MethodContext;
import org.jetbrains.jet.codegen.context.PackageContext;
import org.jetbrains.jet.codegen.state.JetTypeMapper;
import org.jetbrains.jet.descriptors.serialization.descriptors.DeserializedCallableMemberDescriptor;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.codeFragmentUtil.CodeFragmentUtilPackage;
import org.jetbrains.jet.lang.resolve.DescriptorToSourceUtils;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.java.lazy.descriptors.LazyJavaPackageFragment;
import org.jetbrains.jet.lang.resolve.kotlin.KotlinJvmBinaryClass;
import org.jetbrains.jet.lang.resolve.kotlin.VirtualFileKotlinClass;
import org.jetbrains.jet.lang.resolve.kotlin.incremental.IncrementalPackageFragmentProvider;
import org.jetbrains.jet.lang.types.JetType;

import java.io.File;

import static org.jetbrains.jet.lang.descriptors.Modality.ABSTRACT;
import static org.jetbrains.jet.lang.descriptors.Modality.FINAL;

public class JvmCodegenUtil {

    private JvmCodegenUtil() {
    }

    public static boolean isInterface(DeclarationDescriptor descriptor) {
        if (descriptor instanceof ClassDescriptor) {
            ClassKind kind = ((ClassDescriptor) descriptor).getKind();
            return kind == ClassKind.TRAIT || kind == ClassKind.ANNOTATION_CLASS;
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
        if (context == CodegenContext.STATIC) {
            return true;
        }
        DeclarationDescriptor contextDescriptor = context.getContextDescriptor();

        CallableMemberDescriptor directMember = getDirectMember(declarationDescriptor);
        if (directMember instanceof DeserializedCallableMemberDescriptor) {
            return isContainedByCompiledPartOfOurModule(((DeserializedCallableMemberDescriptor) directMember), outDirectory);
        }
        else {
            return DescriptorUtils.areInSameModule(directMember, contextDescriptor);
        }
    }

    private static boolean isContainedByCompiledPartOfOurModule(
            @NotNull DeserializedCallableMemberDescriptor descriptor,
            @Nullable File outDirectory
    ) {
        DeclarationDescriptor packageFragment = descriptor.getContainingDeclaration();
        if (packageFragment instanceof IncrementalPackageFragmentProvider.IncrementalPackageFragment) {
            return true;
        }

        if (outDirectory == null) {
            return false;
        }

        if (!(packageFragment instanceof LazyJavaPackageFragment)) {
            return false;
        }

        KotlinJvmBinaryClass binaryClass = ((LazyJavaPackageFragment) packageFragment).getMemberScope().getKotlinBinaryClass();
        if (binaryClass instanceof VirtualFileKotlinClass) {
            VirtualFile file = ((VirtualFileKotlinClass) binaryClass).getFile();
            if (file.getFileSystem().getProtocol() == StandardFileSystems.FILE_PROTOCOL) {
                File ioFile = VfsUtilCore.virtualToIoFile(file);
                return ioFile.getAbsolutePath().startsWith(outDirectory.getAbsolutePath() + File.separator);
            }
        }

        return false;
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
        if (context.isInlineFunction()) return false;

        // Only properties of the same class can be directly accessed, except when we are evaluating expressions in the debugger
        if (!isCallInsideSameClassAsDeclared(property, context) && !isDebuggerContext(context)) return false;

        // Delegated and extension properties have no backing fields
        if (isDelegated || property.getExtensionReceiverParameter() != null) return false;

        // Class object properties cannot be accessed directly because their backing fields are stored in the containing class
        if (DescriptorUtils.isClassObject(property.getContainingDeclaration())) return false;

        PropertyAccessorDescriptor accessor = forGetter ? property.getGetter() : property.getSetter();

        // If there's no accessor declared we can use direct access
        if (accessor == null) return true;

        // If the accessor is non-default (i.e. it has some code) we should call that accessor and not use direct access
        if (accessor.hasBody()) return false;

        // If the accessor is private or final, it can't be overridden in the subclass and thus we can use direct access
        return property.getVisibility() == Visibilities.PRIVATE || accessor.getModality() == FINAL;
    }

    private static boolean isDebuggerContext(@NotNull MethodContext context) {
        JetFile file = DescriptorToSourceUtils.getContainingFile(context.getContextDescriptor());
        return file != null && CodeFragmentUtilPackage.getSkipVisibilityCheck(file);
    }

    @NotNull
    public static ImplementationBodyCodegen getParentBodyCodegen(@Nullable MemberCodegen<?> classBodyCodegen) {
        assert classBodyCodegen != null && classBodyCodegen.getParentCodegen() instanceof ImplementationBodyCodegen
                : "Class object should have appropriate parent BodyCodegen";

        return (ImplementationBodyCodegen) classBodyCodegen.getParentCodegen();
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
}
