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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.NamespaceDescriptorParent;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.java.*;
import org.jetbrains.jet.lang.resolve.java.descriptor.JavaNamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.java.mapping.JavaToKotlinClassMap;
import org.jetbrains.jet.lang.resolve.java.sam.SingleAbstractMethodUtils;
import org.jetbrains.jet.lang.resolve.java.scope.JavaClassStaticMembersScope;
import org.jetbrains.jet.lang.resolve.java.scope.JavaPackageScope;
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass;
import org.jetbrains.jet.lang.resolve.java.structure.JavaField;
import org.jetbrains.jet.lang.resolve.java.structure.JavaMethod;
import org.jetbrains.jet.lang.resolve.java.structure.JavaPackage;
import org.jetbrains.jet.lang.resolve.kotlin.DeserializedDescriptorResolver;
import org.jetbrains.jet.lang.resolve.kotlin.KotlinClassFinder;
import org.jetbrains.jet.lang.resolve.kotlin.KotlinJvmBinaryClass;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;

import javax.inject.Inject;
import java.util.*;

import static org.jetbrains.jet.lang.resolve.java.DescriptorSearchRule.INCLUDE_KOTLIN_SOURCES;

public final class JavaNamespaceResolver {

    @NotNull
    public static final ModuleDescriptor FAKE_ROOT_MODULE = new ModuleDescriptorImpl(JavaDescriptorResolver.JAVA_ROOT,
                                                                                     JavaBridgeConfiguration.ALL_JAVA_IMPORTS,
                                                                                     JavaToKotlinClassMap.getInstance());
    @NotNull
    private final Map<FqName, JetScope> resolvedNamespaceCache = new HashMap<FqName, JetScope>();
    @NotNull
    private final Set<FqName> unresolvedCache = new HashSet<FqName>();

    private JavaClassFinder javaClassFinder;
    private JavaResolverCache cache;
    private JavaMemberResolver memberResolver;

    private DeserializedDescriptorResolver deserializedDescriptorResolver;
    private KotlinClassFinder kotlinClassFinder;

    @Inject
    public void setKotlinClassFinder(KotlinClassFinder kotlinClassFinder) {
        this.kotlinClassFinder = kotlinClassFinder;
    }

    @Inject
    public void setJavaClassFinder(JavaClassFinder javaClassFinder) {
        this.javaClassFinder = javaClassFinder;
    }

    @Inject
    public void setCache(JavaResolverCache cache) {
        this.cache = cache;
    }

    @Inject
    public void setMemberResolver(@NotNull JavaMemberResolver memberResolver) {
        this.memberResolver = memberResolver;
    }

    @Inject
    public void setDeserializedDescriptorResolver(DeserializedDescriptorResolver deserializedDescriptorResolver) {
        this.deserializedDescriptorResolver = deserializedDescriptorResolver;
    }

    @Nullable
    public NamespaceDescriptor resolveNamespace(@NotNull FqName qualifiedName, @NotNull DescriptorSearchRule searchRule) {
        if (searchRule == INCLUDE_KOTLIN_SOURCES) {
            NamespaceDescriptor kotlinNamespaceDescriptor = cache.getPackageResolvedFromSource(qualifiedName);
            if (kotlinNamespaceDescriptor != null) {
                return kotlinNamespaceDescriptor;
            }
        }

        if (unresolvedCache.contains(qualifiedName)) {
            return null;
        }
        JetScope scope = resolvedNamespaceCache.get(qualifiedName);
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

        JetScope newScope = createNamespaceScope(qualifiedName, javaNamespaceDescriptor, true);
        if (newScope == null) {
            return null;
        }

        javaNamespaceDescriptor.setMemberScope(newScope);

        return javaNamespaceDescriptor;
    }

    @Nullable
    private NamespaceDescriptorParent resolveParentNamespace(@NotNull FqName fqName) {
        if (fqName.isRoot()) {
            return FAKE_ROOT_MODULE;
        }
        else {
            return resolveNamespace(fqName.parent(), INCLUDE_KOTLIN_SOURCES);
        }
    }

    @Nullable
    private JetScope createNamespaceScope(@NotNull FqName fqName, @NotNull NamespaceDescriptor namespaceDescriptor, boolean record) {
        JetScope namespaceScope = doCreateNamespaceScope(fqName, namespaceDescriptor, record);
        cache(fqName, namespaceScope);
        return namespaceScope;
    }

    @Nullable
    private JetScope doCreateNamespaceScope(
            @NotNull FqName fqName,
            @NotNull NamespaceDescriptor namespaceDescriptor,
            boolean record
    ) {
        JavaPackage javaPackage = javaClassFinder.findPackage(fqName);
        if (javaPackage != null) {
            FqName packageClassFqName = PackageClassUtils.getPackageClassFqName(fqName);
            KotlinJvmBinaryClass kotlinClass = kotlinClassFinder.find(packageClassFqName);

            cache.recordProperNamespace(namespaceDescriptor);

            if (kotlinClass != null) {
                JetScope kotlinPackageScope = deserializedDescriptorResolver.createKotlinPackageScope(namespaceDescriptor, kotlinClass);
                if (kotlinPackageScope != null) {
                    return kotlinPackageScope;
                }
            }


            // Otherwise (if psiClass is null or doesn't have a supported Kotlin annotation), it's a Java class and the package is empty
            if (record) {
                cache.recordPackage(javaPackage, namespaceDescriptor);
            }

            return new JavaPackageScope(namespaceDescriptor, javaPackage, fqName, memberResolver);
        }

        JavaClass javaClass = javaClassFinder.findClass(fqName);
        if (javaClass == null) {
            return null;
        }

        if (DescriptorResolverUtils.isCompiledKotlinClassOrPackageClass(javaClass)) {
            return null;
        }
        if (!hasStaticMembers(javaClass)) {
            return null;
        }

        cache.recordClassStaticMembersNamespace(namespaceDescriptor);

        if (record) {
            cache.recordPackage(javaClass, namespaceDescriptor);
        }

        return new JavaClassStaticMembersScope(namespaceDescriptor, fqName, javaClass, memberResolver);
    }

    private void cache(@NotNull FqName fqName, @Nullable JetScope packageScope) {
        if (packageScope == null) {
            unresolvedCache.add(fqName);
            return;
        }
        JetScope oldValue = resolvedNamespaceCache.put(fqName, packageScope);
        if (oldValue != null) {
            throw new IllegalStateException("rewrite at " + fqName);
        }
    }

    @Nullable
    public JetScope getJavaPackageScopeForExistingNamespaceDescriptor(@NotNull NamespaceDescriptor namespaceDescriptor) {
        FqName fqName = DescriptorUtils.getFQName(namespaceDescriptor).toSafe();
        if (unresolvedCache.contains(fqName)) {
            throw new IllegalStateException(
                    "This means that we are trying to create a Java package, but have a package with the same FQN defined in Kotlin: " +
                    fqName);
        }
        JetScope alreadyResolvedScope = resolvedNamespaceCache.get(fqName);
        if (alreadyResolvedScope != null) {
            return alreadyResolvedScope;
        }
        return createNamespaceScope(fqName, namespaceDescriptor, false);
    }

    private static boolean hasStaticMembers(@NotNull JavaClass javaClass) {
        for (JavaMethod method : javaClass.getMethods()) {
            if (method.isStatic() && !DescriptorResolverUtils.shouldBeInEnumClassObject(method)) {
                return true;
            }
        }

        for (JavaField field : javaClass.getAllFields()) {
            if (field.isStatic() && !DescriptorResolverUtils.shouldBeInEnumClassObject(field)) {
                return true;
            }
        }

        for (JavaClass nestedClass : javaClass.getInnerClasses()) {
            if (SingleAbstractMethodUtils.isSamInterface(nestedClass)) {
                return true;
            }
            if (nestedClass.isStatic() && hasStaticMembers(nestedClass)) {
                return true;
            }
        }

        return false;
    }

    @NotNull
    public Collection<Name> getClassNamesInPackage(@NotNull FqName packageName) {
        JavaPackage javaPackage = javaClassFinder.findPackage(packageName);
        if (javaPackage == null) return Collections.emptyList();

        Collection<JavaClass> classes = DescriptorResolverUtils.getClassesInPackage(javaPackage);
        List<Name> result = new ArrayList<Name>(classes.size());
        for (JavaClass javaClass : classes) {
            if (DescriptorResolverUtils.isCompiledKotlinClass(javaClass)) {
                result.add(javaClass.getName());
            }
        }

        return result;
    }
}
