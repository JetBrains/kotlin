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

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.NotNullFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.PackageLikeDescriptorBase;
import org.jetbrains.jet.lang.resolve.java.kt.JetPackageClassAnnotation;
import org.jetbrains.jet.lang.resolve.java.provider.PsiDeclarationProviderFactory;
import org.jetbrains.jet.lang.resolve.java.scope.JavaClassStaticMembersScope;
import org.jetbrains.jet.lang.resolve.java.scope.JavaPackageScopeWithoutMembers;
import org.jetbrains.jet.lang.resolve.java.scope.JavaScopeForKotlinNamespace;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;

import java.util.Collection;
import java.util.Collections;

public class JavaPackageFragmentProvider implements PackageFragmentProvider {

    public static final PackageFragmentKind JAVA = new PackageFragmentKind() {
        @Override
        public String toString() {
            return "JAVA";
        }
    };

    private final JavaSemanticServices javaSemanticServices;
    private final PsiDeclarationProviderFactory declarationProviderFactory;
    private final GlobalSearchScope definingSearchScope;
    private final SubModuleDescriptor subModule;

    protected JavaPackageFragmentProvider(
            @NotNull JavaSemanticServices javaSemanticServices,
            @NotNull PsiDeclarationProviderFactory declarationProviderFactory,
            @NotNull GlobalSearchScope definingSearchScope,
            @NotNull SubModuleDescriptor subModule
    ) {
        this.javaSemanticServices = javaSemanticServices;
        this.declarationProviderFactory = declarationProviderFactory;
        this.definingSearchScope = definingSearchScope;
        this.subModule = subModule;
    }

    @NotNull
    public GlobalSearchScope getDefiningSearchScope() {
        return definingSearchScope;
    }

    @NotNull
    public ClassDescriptor getClassDescriptor(@NotNull PsiClass psiClass) {
        assertInMyScope(psiClass);

        psiClass.getContainingFile().
    }

    protected void assertInMyScope(@NotNull PsiClass psiClass) {
        VirtualFile file = psiClass.getContainingFile().getVirtualFile();
        assert file != null : "No virtual file for psiClass: " + psiClass.getText();
        assert definingSearchScope.contains(file) : "Not in scope\n psiClass " + psiClass.getText() + "\nscope: " + definingSearchScope;
    }

    @NotNull
    @Override
    public Collection<PackageFragmentDescriptor> getPackageFragments(@NotNull final FqName fqName) {
        JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(definingSearchScope.getProject());

        final PsiClass staticClass = javaPsiFacade.findClass(fqName.getFqName(), definingSearchScope);
        if (staticClass != null) {
            if (staticClass.isEnum()) {
                // TODO: this is a bug. but reproduces the existing behavior
                // http://youtrack.jetbrains.com/issue/KT-3377

                // old comment:
                // NOTE: we don't want to create namespace for enum classes because we put
                // static members of enum class into class object descriptor
                return Collections.emptyList();
            }

            return Collections.singletonList(createPackageFragmentForStaticClass(fqName, staticClass));
        }

        final PsiPackage psiPackage = javaPsiFacade.findPackage(fqName.getFqName());
        if (psiPackage == null) return Collections.emptyList();

        PsiClass[] classes = psiPackage.getClasses(definingSearchScope);
        if (classes.length == 0) return Collections.emptyList();

        final PsiClass packageClass = javaPsiFacade.findClass(PackageClassUtils.getPackageClassFqName(fqName).getFqName(), definingSearchScope);

        if (packageClass == null) {
            return Collections.singletonList(createPackageFragmentForPackageWithoutMembers(fqName, psiPackage));
        }

        AbiVersionUtil.checkAbiVersion(packageClass, JetPackageClassAnnotation.get(packageClass), javaSemanticServices.getDiagnosticHolder());
        return Collections.singletonList(createPackageFragmentForPackageWithMembers(fqName, psiPackage, packageClass));
    }

    private PackageFragmentDescriptor createPackageFragmentForStaticClass(final FqName fqName, final PsiClass staticClass) {
        return new JavaPackageFragment(subModule, fqName,
                                       new NotNullFunction<PackageFragmentDescriptor, JetScope>() {
                                           @NotNull
                                           @Override
                                           public JetScope fun(PackageFragmentDescriptor fragment) {
                                               return new JavaClassStaticMembersScope(
                                                       fragment,
                                                       declarationProviderFactory.createDeclarationProviderForClassStaticMembers(
                                                               staticClass),
                                                       fqName,
                                                       javaSemanticServices);
                                           }
                                       });
    }

    private PackageFragmentDescriptor createPackageFragmentForPackageWithoutMembers(
            final FqName fqName,
            final PsiPackage psiPackage
    ) {
        return new JavaPackageFragment(subModule, fqName,
                                       new NotNullFunction<PackageFragmentDescriptor, JetScope>() {
                                           @NotNull
                                           @Override
                                           public JetScope fun(PackageFragmentDescriptor fragment) {
                                               return new JavaPackageScopeWithoutMembers(
                                                                   fragment,
                                                                   declarationProviderFactory.createDeclarationProviderForNamespaceWithoutMembers(
                                                                           psiPackage),
                                                                   fqName, javaSemanticServices);
                                           }
                                       });
    }

    private PackageFragmentDescriptor createPackageFragmentForPackageWithMembers(
            final FqName fqName,
            final PsiPackage psiPackage,
            final PsiClass packageClass
    ) {
        return new JavaPackageFragment(subModule, fqName,
                                       new NotNullFunction<PackageFragmentDescriptor, JetScope>() {
                                           @NotNull
                                           @Override
                                           public JetScope fun(PackageFragmentDescriptor fragment) {
                                               return new JavaScopeForKotlinNamespace(
                                                       fragment,
                                                       declarationProviderFactory.createDeclarationForKotlinNamespace(
                                                               psiPackage, packageClass),
                                                       fqName, javaSemanticServices);
                                           }
                                       });
    }

    private static class JavaPackageFragment extends PackageLikeDescriptorBase implements PackageFragmentDescriptor {

        private final SubModuleDescriptor subModule;
        private final JetScope memberScope;

        public JavaPackageFragment(@NotNull SubModuleDescriptor subModule, @NotNull FqName fqName, @NotNull NotNullFunction<PackageFragmentDescriptor, JetScope> memberScope) {
            super(fqName);
            this.subModule = subModule;
            this.memberScope = memberScope.fun(this);
        }

        @NotNull
        @Override
        public PackageFragmentKind getKind() {
            return JAVA;
        }

        @NotNull
        @Override
        public SubModuleDescriptor getContainingDeclaration() {
            return subModule;
        }

        @NotNull
        @Override
        public JetScope getMemberScope() {
            return memberScope;
        }

        @Override
        public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
            return visitor.visitPackageFragmentDescriptor(this, data);
        }
    }
}
