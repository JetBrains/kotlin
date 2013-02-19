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

package org.jetbrains.jet.lang.resolve.java.resolver;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.NamespaceDescriptorParent;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.java.*;
import org.jetbrains.jet.lang.resolve.java.descriptor.JavaNamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.java.kt.JetPackageClassAnnotation;
import org.jetbrains.jet.lang.resolve.java.scope.JavaBaseScope;
import org.jetbrains.jet.lang.resolve.java.scope.JavaClassStaticMembersScope;
import org.jetbrains.jet.lang.resolve.java.scope.JavaPackageScopeWithoutMembers;
import org.jetbrains.jet.lang.resolve.java.scope.JavaScopeForKotlinNamespace;
import org.jetbrains.jet.lang.resolve.name.FqName;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public final class JavaNamespaceResolver {

    @NotNull
    public static final ModuleDescriptor FAKE_ROOT_MODULE = new ModuleDescriptor(JavaDescriptorResolver.JAVA_ROOT);
    @NotNull
    private final Map<FqName, JavaBaseScope> resolvedNamespaceCache = Maps.newHashMap();
    @NotNull
    private final Set<FqName> unresolvedCache = Sets.newHashSet();

    private PsiClassFinder psiClassFinder;
    private BindingTrace trace;
    private JavaSemanticServices javaSemanticServices;

    public JavaNamespaceResolver() {
    }

    @Inject
    public void setPsiClassFinder(PsiClassFinder psiClassFinder) {
        this.psiClassFinder = psiClassFinder;
    }

    @Inject
    public void setTrace(BindingTrace trace) {
        this.trace = trace;
    }

    @Inject
    public void setJavaSemanticServices(JavaSemanticServices javaSemanticServices) {
        this.javaSemanticServices = javaSemanticServices;
    }

    @Nullable
    public NamespaceDescriptor resolveNamespace(@NotNull FqName qualifiedName, @NotNull DescriptorSearchRule searchRule) {
        // First, let's check that there is no Kotlin package:
        NamespaceDescriptor kotlinNamespaceDescriptor = javaSemanticServices.getKotlinNamespaceDescriptor(qualifiedName);
        if (kotlinNamespaceDescriptor != null) {
            return searchRule.processFoundInKotlin(kotlinNamespaceDescriptor);
        }

        if (unresolvedCache.contains(qualifiedName)) {
            return null;
        }
        JavaBaseScope scope = resolvedNamespaceCache.get(qualifiedName);
        if (scope != null) {
            return (NamespaceDescriptor) scope.getContainingDeclaration();
        }

        NamespaceDescriptorParent parentNs = resolveParentNamespace(qualifiedName);
        if (parentNs == null) {
            return null;
        }

        JavaNamespaceDescriptor javaNamespaceDescriptor = new JavaNamespaceDescriptor(
                parentNs,
                Collections.<AnnotationDescriptor>emptyList(), // TODO
                qualifiedName
        );

        JavaBaseScope newScope = createNamespaceScope(qualifiedName, javaNamespaceDescriptor);
        if (newScope == null) {
            return null;
        }

        trace.record(BindingContext.NAMESPACE, newScope.getPsiElement(), javaNamespaceDescriptor);

        javaNamespaceDescriptor.setMemberScope(newScope);

        return javaNamespaceDescriptor;
    }

    @Nullable
    public NamespaceDescriptor resolveNamespace(@NotNull FqName qualifiedName) {
        return resolveNamespace(qualifiedName, DescriptorSearchRule.ERROR_IF_FOUND_IN_KOTLIN);
    }

    @Nullable
    private NamespaceDescriptorParent resolveParentNamespace(@NotNull FqName fqName) {
        if (fqName.isRoot()) {
            return FAKE_ROOT_MODULE;
        }
        else {
            return resolveNamespace(fqName.parent(), DescriptorSearchRule.INCLUDE_KOTLIN);
        }
    }

    @Nullable
    private JavaBaseScope createNamespaceScope(
            @NotNull FqName fqName,
            @NotNull NamespaceDescriptor namespaceDescriptor
    ) {
        JavaBaseScope namespaceScope = doCreateNamespaceScope(fqName, namespaceDescriptor);
        cache(fqName, namespaceScope);
        return namespaceScope;
    }

    @Nullable
    private JavaBaseScope doCreateNamespaceScope(
            @NotNull FqName fqName,
            @NotNull NamespaceDescriptor namespaceDescriptor
    ) {
        PsiPackage psiPackage = psiClassFinder.findPsiPackage(fqName);
        if (psiPackage != null) {
            PsiClass psiClass = getPsiClassForJavaPackageScope(fqName);
            trace.record(JavaBindingContext.JAVA_NAMESPACE_KIND, namespaceDescriptor, JavaNamespaceKind.PROPER);
            if (psiClass == null) {
                return new JavaPackageScopeWithoutMembers(
                        namespaceDescriptor,
                        javaSemanticServices.getPsiDeclarationProviderFactory().createDeclarationProviderForNamespaceWithoutMembers(psiPackage),
                        fqName, javaSemanticServices);
            }

            AbiVersionUtil.checkAbiVersion(psiClass, JetPackageClassAnnotation.get(psiClass), trace);
            return new JavaScopeForKotlinNamespace(
                    namespaceDescriptor,
                    javaSemanticServices.getPsiDeclarationProviderFactory().createDeclarationForKotlinNamespace(psiPackage, psiClass),
                    fqName, javaSemanticServices);
        }

        PsiClass psiClass = psiClassFinder.findPsiClass(fqName, PsiClassFinder.RuntimeClassesHandleMode.IGNORE);
        if (psiClass == null) {
            return null;
        }
        if (psiClass.isEnum()) {
            // NOTE: we don't want to create namespace for enum classes because we put
            // static members of enum class into class object descriptor
            return null;
        }
        trace.record(JavaBindingContext.JAVA_NAMESPACE_KIND, namespaceDescriptor, JavaNamespaceKind.CLASS_STATICS);
        return new JavaClassStaticMembersScope(
                namespaceDescriptor,
                javaSemanticServices.getPsiDeclarationProviderFactory().createDeclarationProviderForClassStaticMembers(psiClass),
                fqName, javaSemanticServices);
    }

    private void cache(@NotNull FqName fqName, @Nullable JavaBaseScope packageScope) {
        if (packageScope == null) {
            unresolvedCache.add(fqName);
            return;
        }
        JavaBaseScope oldValue = resolvedNamespaceCache.put(fqName, packageScope);
        if (oldValue != null) {
            throw new IllegalStateException("rewrite at " + fqName);
        }
    }

    @Nullable
    public JavaBaseScope getJavaPackageScopeForExistingNamespaceDescriptor(@NotNull NamespaceDescriptor namespaceDescriptor) {
        FqName fqName = DescriptorUtils.getFQName(namespaceDescriptor).toSafe();
        if (unresolvedCache.contains(fqName)) {
            throw new IllegalStateException(
                    "This means that we are trying to create a Java package, but have a package with the same FQN defined in Kotlin: " +
                    fqName);
        }
        JavaBaseScope alreadyResolvedScope = resolvedNamespaceCache.get(fqName);
        if (alreadyResolvedScope != null) {
            return alreadyResolvedScope;
        }
        return createNamespaceScope(fqName, namespaceDescriptor);
    }

    @Nullable
    private PsiClass getPsiClassForJavaPackageScope(@NotNull FqName packageFQN) {
        return psiClassFinder.findPsiClass(PackageClassUtils.getPackageClassFqName(packageFQN), PsiClassFinder.RuntimeClassesHandleMode.IGNORE);
    }
}