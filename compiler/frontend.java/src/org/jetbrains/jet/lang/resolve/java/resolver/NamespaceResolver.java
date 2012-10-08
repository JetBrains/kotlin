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
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptorParent;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.*;
import org.jetbrains.jet.lang.resolve.java.descriptor.JavaNamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.java.scope.JavaPackageScope;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.Collections;
import java.util.Map;

public final class NamespaceResolver {

    private final JavaDescriptorResolver javaDescriptorResolver;
    private final Map<FqName, JavaDescriptorResolveData.ResolverNamespaceData> namespaceDescriptorCacheByFqn = Maps.newHashMap();

    public NamespaceResolver(JavaDescriptorResolver javaDescriptorResolver) {
        this.javaDescriptorResolver = javaDescriptorResolver;
    }

    @Nullable
    public NamespaceDescriptor resolveNamespace(@NotNull FqName qualifiedName, @NotNull DescriptorSearchRule searchRule) {
        // First, let's check that there is no Kotlin package:
        NamespaceDescriptor kotlinNamespaceDescriptor =
                javaDescriptorResolver.getSemanticServices().getKotlinNamespaceDescriptor(qualifiedName);
        if (kotlinNamespaceDescriptor != null) {
            if (searchRule == DescriptorSearchRule.ERROR_IF_FOUND_IN_KOTLIN) {
                throw new IllegalStateException("class must not be found in kotlin: " + qualifiedName);
            }
            else if (searchRule == DescriptorSearchRule.IGNORE_IF_FOUND_IN_KOTLIN) {
                return null;
            }
            else if (searchRule == DescriptorSearchRule.INCLUDE_KOTLIN) {
                // TODO: probably this is evil
                return kotlinNamespaceDescriptor;
            }
            else {
                throw new IllegalStateException("unknown searchRule: " + searchRule);
            }
        }

        JavaDescriptorResolveData.ResolverNamespaceData namespaceData = namespaceDescriptorCacheByFqn.get(qualifiedName);
        if (namespaceData != null) {
            return namespaceData.getNamespaceDescriptor();
        }

        NamespaceDescriptorParent parentNs = resolveParentNamespace(qualifiedName);
        if (parentNs == null) {
            return null;
        }

        JavaNamespaceDescriptor ns = new JavaNamespaceDescriptor(
                parentNs,
                Collections.<AnnotationDescriptor>emptyList(), // TODO
                qualifiedName
        );

        JavaDescriptorResolveData.ResolverNamespaceData scopeData = createNamespaceResolverScopeData(qualifiedName, ns);
        if (scopeData == null) {
            return null;
        }

        javaDescriptorResolver.getTrace().record(BindingContext.NAMESPACE, scopeData.getPsiPackageOrPsiClass(), ns);

        ns.setMemberScope(scopeData.getMemberScope());

        return scopeData.getNamespaceDescriptor();
    }

    public NamespaceDescriptor resolveNamespace(@NotNull FqName qualifiedName) {
        return resolveNamespace(qualifiedName, DescriptorSearchRule.ERROR_IF_FOUND_IN_KOTLIN);
    }

    private NamespaceDescriptorParent resolveParentNamespace(FqName fqName) {
        if (fqName.isRoot()) {
            return JavaDescriptorResolver.FAKE_ROOT_MODULE;
        }
        else {
            return resolveNamespace(fqName.parent(), DescriptorSearchRule.INCLUDE_KOTLIN);
        }
    }

    @Nullable
    private JavaDescriptorResolveData.ResolverNamespaceData createNamespaceResolverScopeData(
            @NotNull FqName fqName,
            @NotNull NamespaceDescriptor ns
    ) {
        PsiPackage psiPackage;
        PsiClass psiClass;

        lookingForPsi:
        {
            psiClass = getPsiClassForJavaPackageScope(fqName);
            psiPackage = javaDescriptorResolver.getSemanticServices().getPsiClassFinder().findPsiPackage(fqName);
            if (psiClass != null || psiPackage != null) {
                javaDescriptorResolver.getTrace().record(JavaBindingContext.JAVA_NAMESPACE_KIND, ns, JavaNamespaceKind.PROPER);
                break lookingForPsi;
            }

            psiClass = javaDescriptorResolver.getPsiClassFinder().findPsiClass(fqName, PsiClassFinder.RuntimeClassesHandleMode.IGNORE);
            if (psiClass != null && !psiClass.isEnum()) {
                javaDescriptorResolver.getTrace().record(JavaBindingContext.JAVA_NAMESPACE_KIND, ns, JavaNamespaceKind.CLASS_STATICS);
                break lookingForPsi;
            }

            JavaDescriptorResolveData.ResolverNamespaceData oldValue =
                    namespaceDescriptorCacheByFqn.put(fqName, JavaDescriptorResolveData.ResolverNamespaceData.NEGATIVE);
            if (oldValue != null) {
                throw new IllegalStateException("rewrite at " + fqName);
            }
            return null;
        }

        JavaDescriptorResolveData.ResolverNamespaceData namespaceData =
                new JavaDescriptorResolveData.ResolverNamespaceData(psiClass, psiPackage, fqName, ns);

        namespaceData.setMemberScope(new JavaPackageScope(fqName, javaDescriptorResolver.getSemanticServices(), namespaceData));

        JavaDescriptorResolveData.ResolverNamespaceData oldValue =
                namespaceDescriptorCacheByFqn.put(fqName, namespaceData);
        if (oldValue != null) {
            throw new IllegalStateException("rewrite at " + fqName);
        }

        return namespaceData;
    }

    @Nullable
    public JavaPackageScope getJavaPackageScope(@NotNull FqName fqName, @NotNull NamespaceDescriptor ns) {
        JavaDescriptorResolveData.ResolverNamespaceData resolverNamespaceData = namespaceDescriptorCacheByFqn.get(fqName);
        if (resolverNamespaceData == null) {
            resolverNamespaceData = createNamespaceResolverScopeData(fqName, ns);
        }
        if (resolverNamespaceData == null) {
            return null;
        }
        if (resolverNamespaceData == JavaDescriptorResolveData.ResolverNamespaceData.NEGATIVE) {
            throw new IllegalStateException(
                    "This means that we are trying to create a Java package, but have a package with the same FQN defined in Kotlin: " +
                    fqName);
        }
        JavaPackageScope scope = resolverNamespaceData.getMemberScope();
        if (scope == null) {
            throw new IllegalStateException("fqn: " + fqName);
        }
        return scope;
    }

    @Nullable
    private PsiClass getPsiClassForJavaPackageScope(@NotNull FqName packageFQN) {
        return javaDescriptorResolver.getPsiClassFinder()
                .findPsiClass(packageFQN.child(Name.identifier(JvmAbi.PACKAGE_CLASS)), PsiClassFinder.RuntimeClassesHandleMode.IGNORE);
    }
}