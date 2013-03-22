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

package org.jetbrains.jet.lang.resolve.java;

import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.java.provider.ClassPsiDeclarationProvider;
import org.jetbrains.jet.lang.resolve.java.provider.PsiDeclarationProvider;
import org.jetbrains.jet.lang.resolve.java.resolver.*;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class JavaDescriptorResolver {

    public static final Name JAVA_ROOT = Name.special("<java_root>");

    public static final Visibility PACKAGE_VISIBILITY = new Visibility("package", false) {
        @Override
        protected boolean isVisible(@NotNull DeclarationDescriptorWithVisibility what, @NotNull DeclarationDescriptor from) {
            return DescriptorUtils.isInSameNamespace(what, from);
        }

        @Override
        protected Integer compareTo(@NotNull Visibility visibility) {
            if (this == visibility) return 0;
            if (visibility == Visibilities.PRIVATE) return 1;
            return -1;
        }

        @Override
        public String toString() {
            return "public/*package*/";
        }

        @NotNull
        @Override
        public Visibility normalize() {
            return Visibilities.INTERNAL;
        }
    };

    public static final Visibility PROTECTED_STATIC_VISIBILITY = new Visibility("protected_static", false) {
        @Override
        protected boolean isVisible(@NotNull DeclarationDescriptorWithVisibility what, @NotNull DeclarationDescriptor from) {
            ClassDescriptor fromClass = DescriptorUtils.getParentOfType(from, ClassDescriptor.class, false);
            if (fromClass == null) return false;

            ClassDescriptor whatClass;
            // protected static class
            if (what instanceof ClassDescriptor) {
                DeclarationDescriptor containingDeclaration = what.getContainingDeclaration();
                assert containingDeclaration instanceof ClassDescriptor : "Only static nested classes can have protected_static visibility";
                whatClass = (ClassDescriptor) containingDeclaration;
            }
            // protected static function or property
            else {
                assert DescriptorUtils.isTopLevelDeclaration(what) : "Only static declarations can have protected_static visibility";
                DeclarationDescriptor whatDeclarationDescriptor = DescriptorUtils.getParentInPackageViewHierarchy(what);
                whatClass = DescriptorUtils.getClassForCorrespondingJavaNamespace((PackageViewDescriptor) whatDeclarationDescriptor);
            }

            assert whatClass != null : "Couldn't find ClassDescriptor for protected static member " + what;

            if (DescriptorUtils.isSubclass(fromClass, whatClass)) {
                return true;
            }
            return isVisible(what, fromClass.getContainingDeclaration());
        }

        @Override
        public String toString() {
            return "protected/*protected static*/";
        }

        @NotNull
        @Override
        public Visibility normalize() {
            return Visibilities.PROTECTED;
        }
    };

    public static final Visibility PROTECTED_AND_PACKAGE = new Visibility("protected_and_package", false) {
        @Override
        protected boolean isVisible(@NotNull DeclarationDescriptorWithVisibility what, @NotNull DeclarationDescriptor from) {
            if (DescriptorUtils.isInSameNamespace(what, from)) {
                return true;
            }

            ClassDescriptor whatClass = DescriptorUtils.getParentOfType(what, ClassDescriptor.class, false);
            if (whatClass == null) return false;

            ClassDescriptor fromClass = DescriptorUtils.getParentOfType(from, ClassDescriptor.class, false);
            if (fromClass == null) return false;

            if (DescriptorUtils.isSubclass(fromClass, whatClass)) {
                return true;
            }
            return isVisible(what, fromClass.getContainingDeclaration());
        }

        @Override
        protected Integer compareTo(@NotNull Visibility visibility) {
            if (this == visibility) return 0;
            if (visibility == Visibilities.INTERNAL) return null;
            if (visibility == Visibilities.PRIVATE) return 1;
            return -1;
        }

        @Override
        public String toString() {
            return "protected/*protected and package*/";
        }

        @NotNull
        @Override
        public Visibility normalize() {
            return Visibilities.PROTECTED;
        }
    };

    private JavaPropertyResolver propertiesResolver;
    private JavaClassResolver classResolver;
    private JavaConstructorResolver constructorResolver;
    private JavaFunctionResolver functionResolver;
    private JavaInnerClassResolver innerClassResolver;
    private JavaClassResolutionFacade classResolutionFacade;

    @Inject
    public void setFunctionResolver(JavaFunctionResolver functionResolver) {
        this.functionResolver = functionResolver;
    }

    @Inject
    public void setClassResolver(JavaClassResolver classResolver) {
        this.classResolver = classResolver;
    }

    @Inject
    public void setPropertiesResolver(JavaPropertyResolver propertiesResolver) {
        this.propertiesResolver = propertiesResolver;
    }

    @Inject
    public void setConstructorResolver(JavaConstructorResolver constructorResolver) {
        this.constructorResolver = constructorResolver;
    }

    @Inject
    public void setInnerClassResolver(JavaInnerClassResolver innerClassResolver) {
        this.innerClassResolver = innerClassResolver;
    }

    @Inject
    public void setClassResolutionFacade(JavaClassResolutionFacade classResolutionFacade) {
        this.classResolutionFacade = classResolutionFacade;
    }

    @Nullable
    public ClassDescriptor resolveClass(@NotNull PsiClass psiClass, @NotNull DescriptorSearchRule searchRule) {
        return classResolver.resolveClass(psiClass, searchRule);
    }

    @Nullable
    public ClassDescriptor resolveClass(@NotNull PsiClass psiClass) {
        return classResolver.resolveClass(psiClass);
    }

    @NotNull
    public Collection<ConstructorDescriptor> resolveConstructors(
            @NotNull ClassPsiDeclarationProvider classData, @NotNull ClassDescriptor classDescriptor
    ) {
        return constructorResolver.resolveConstructors(classData, classDescriptor);
    }

    @Deprecated
    @Nullable
    public PackageViewDescriptor resolveNamespace(@NotNull FqName qualifiedName, @NotNull DescriptorSearchRule searchRule) {
        //return namespaceResolver.resolveNamespace(qualifiedName, searchRule);
        return null;
    }

    @Deprecated
    public PackageViewDescriptor resolveNamespace(@NotNull FqName qualifiedName) {
        //return namespaceResolver.resolveNamespace(qualifiedName);
        return null;
    }

    @Deprecated
    @Nullable
    public JetScope getJavaPackageScope(@NotNull PackageViewDescriptor packageViewDescriptor) {
        //return namespaceResolver.getJavaPackageScopeForExistingNamespaceDescriptor(packageViewDescriptor);
        return null;
    }

    @NotNull
    public Set<VariableDescriptor> resolveFieldGroupByName(
            @NotNull Name name,
            @NotNull PsiDeclarationProvider data,
            @NotNull ClassOrPackageDescriptor ownerDescriptor
    ) {
        return propertiesResolver.resolveFieldGroupByName(name, data, ownerDescriptor);
    }

    @Nullable
    public ClassDescriptor getClassDescriptor(@NotNull PsiClass psiClass) {
        return classResolutionFacade.getClassDescriptor(psiClass);
    }

    public static class ValueParameterDescriptors {
        private final JetType receiverType;
        private final List<ValueParameterDescriptor> descriptors;

        public ValueParameterDescriptors(@Nullable JetType receiverType, @NotNull List<ValueParameterDescriptor> descriptors) {
            this.receiverType = receiverType;
            this.descriptors = descriptors;
        }

        @Nullable
        public JetType getReceiverType() {
            return receiverType;
        }

        @NotNull
        public List<ValueParameterDescriptor> getDescriptors() {
            return descriptors;
        }
    }

    @NotNull
    public Set<FunctionDescriptor> resolveFunctionGroup(
            @NotNull Name methodName,
            @NotNull ClassPsiDeclarationProvider scopeData,
            @NotNull ClassOrPackageDescriptor ownerDescriptor
    ) {
        return functionResolver.resolveFunctionGroup(methodName, scopeData, ownerDescriptor);
    }

    @NotNull
    public List<ClassDescriptor> resolveInnerClasses(@NotNull ClassPsiDeclarationProvider declarationProvider) {
        return innerClassResolver.resolveInnerClasses(declarationProvider);
    }
}
