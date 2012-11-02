/*
 * Copyright 2010-2012 JetBrains s.r.o.
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
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptorParent;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.java.*;
import org.jetbrains.jet.lang.resolve.java.data.ResolverNamespaceData;
import org.jetbrains.jet.lang.resolve.java.descriptor.JavaNamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.java.scope.JavaPackageScope;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public final class JavaNamespaceResolver {

    @NotNull
    public static final ModuleDescriptor FAKE_ROOT_MODULE = new ModuleDescriptor(JavaDescriptorResolver.JAVA_ROOT);
    @NotNull
    private final Map<FqName, JavaPackageScope> resolvedNamespaceCache = Maps.newHashMap();
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
        JavaPackageScope scope = resolvedNamespaceCache.get(qualifiedName);
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

        JavaPackageScope newScope = createNamespaceScope(qualifiedName, javaNamespaceDescriptor);
        if (newScope == null) {
            return null;
        }

        trace.record(BindingContext.NAMESPACE, newScope.getResolverScopeData().getPsiPackageOrPsiClass(), javaNamespaceDescriptor);

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
    private JavaPackageScope createNamespaceScope(
            @NotNull FqName fqName,
            @NotNull NamespaceDescriptor namespaceDescriptor
    ) {
        ResolverNamespaceData namespaceData = createNamespaceData(fqName, namespaceDescriptor);
        JavaPackageScope javaPackageScope;
        if (namespaceData == null) {
            javaPackageScope = null;
        }
        else {
            javaPackageScope = new JavaPackageScope(fqName, javaSemanticServices, namespaceData);
        }

        cache(fqName, javaPackageScope);

        return javaPackageScope;
    }

    @Nullable
    private ResolverNamespaceData createNamespaceData(
            @NotNull FqName fqName,
            @NotNull NamespaceDescriptor namespaceDescriptor
    ) {
        PsiPackage psiPackage;
        PsiClass psiClass;

        lookingForPsi:
        {
            psiClass = getPsiClassForJavaPackageScope(fqName);
            psiPackage = psiClassFinder.findPsiPackage(fqName);
            if (psiClass != null || psiPackage != null) {
                trace.record(JavaBindingContext.JAVA_NAMESPACE_KIND, namespaceDescriptor, JavaNamespaceKind.PROPER);
                break lookingForPsi;
            }

            psiClass = psiClassFinder.findPsiClass(fqName, PsiClassFinder.RuntimeClassesHandleMode.IGNORE);
            if (psiClass != null && !psiClass.isEnum()) {
                trace.record(JavaBindingContext.JAVA_NAMESPACE_KIND, namespaceDescriptor, JavaNamespaceKind.CLASS_STATICS);
                break lookingForPsi;
            }
            return null;
        }

        return new ResolverNamespaceData(psiClass, psiPackage, fqName, namespaceDescriptor);
    }

    private void cache(@NotNull FqName fqName, @Nullable JavaPackageScope packageScope) {
        if (packageScope == null) {
            unresolvedCache.add(fqName);
            return;
        }
        JavaPackageScope oldValue = resolvedNamespaceCache.put(fqName, packageScope);
        if (oldValue != null) {
            throw new IllegalStateException("rewrite at " + fqName);
        }
    }

    @Nullable
    public JavaPackageScope getJavaPackageScopeForExistingNamespaceDescriptor(@NotNull NamespaceDescriptor namespaceDescriptor) {
        FqName fqName = DescriptorUtils.getFQName(namespaceDescriptor).toSafe();
        if (unresolvedCache.contains(fqName)) {
            throw new IllegalStateException(
                    "This means that we are trying to create a Java package, but have a package with the same FQN defined in Kotlin: " +
                    fqName);
        }
        JavaPackageScope alreadyResolvedScope = resolvedNamespaceCache.get(fqName);
        if (alreadyResolvedScope != null) {
            return alreadyResolvedScope;
        }
        return createNamespaceScope(fqName, namespaceDescriptor);
    }

    @Nullable
    private PsiClass getPsiClassForJavaPackageScope(@NotNull FqName packageFQN) {
        return psiClassFinder.findPsiClass(packageFQN.child(Name.identifier(JvmAbi.PACKAGE_CLASS)),
                                           PsiClassFinder.RuntimeClassesHandleMode.IGNORE);
    }
}