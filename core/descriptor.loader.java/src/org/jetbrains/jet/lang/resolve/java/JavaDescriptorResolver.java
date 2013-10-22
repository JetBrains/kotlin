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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.PlatformToKotlinClassMap;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptorImpl;
import org.jetbrains.jet.lang.resolve.ImportPath;
import org.jetbrains.jet.lang.resolve.java.lazy.LazyJavaClassResolver;
import org.jetbrains.jet.lang.resolve.java.lazy.LazyJavaResolverContext;
import org.jetbrains.jet.lang.resolve.java.lazy.LazyJavaSubModule;
import org.jetbrains.jet.lang.resolve.java.resolver.ExternalAnnotationResolver;
import org.jetbrains.jet.lang.resolve.java.resolver.JavaClassResolver;
import org.jetbrains.jet.lang.resolve.java.resolver.JavaPackageFragmentProvider;
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.DependencyClassByQualifiedNameResolver;
import org.jetbrains.jet.storage.LockBasedStorageManager;

import javax.inject.Inject;

import java.util.Collections;

import static org.jetbrains.jet.lang.resolve.java.DescriptorSearchRule.IGNORE_KOTLIN_SOURCES;

public class JavaDescriptorResolver implements DependencyClassByQualifiedNameResolver {
    private final boolean LAZY;

    {
        LAZY = true;
    }

    public static final Name JAVA_ROOT = Name.special("<java_root>");

    private JavaClassResolver classResolver;
    private JavaPackageFragmentProvider packageFragmentProvider;
    private JavaClassFinder javaClassFinder;
    private ExternalAnnotationResolver externalAnnotationResolver;
    private LazyJavaSubModule subModule;

    @Inject
    public void setClassResolver(JavaClassResolver classResolver) {
        this.classResolver = classResolver;
    }

    @Inject
    public void setPackageFragmentProvider(JavaPackageFragmentProvider packageFragmentProvider) {
        this.packageFragmentProvider = packageFragmentProvider;
    }

    @Inject
    public void setJavaClassFinder(JavaClassFinder javaClassFinder) {
        this.javaClassFinder = javaClassFinder;
    }

    @Inject
    public void setExternalAnnotationResolver(ExternalAnnotationResolver externalAnnotationResolver) {
        this.externalAnnotationResolver = externalAnnotationResolver;
    }

    @NotNull
    private LazyJavaSubModule getSubModule() {
        if (subModule == null) {
            subModule = new LazyJavaSubModule(
                    new LazyJavaResolverContext(
                            new LockBasedStorageManager(),
                            javaClassFinder,
                            new LazyJavaClassResolver() {
                                @Override
                                public ClassDescriptor resolveClass(JavaClass aClass) {
                                    return null;
                                }

                                @Override
                                public ClassDescriptor resolveClassByFqName(FqName name) {
                                    return null;
                                }
                            },
                            externalAnnotationResolver
                    ),
                    new ModuleDescriptorImpl(Name.special("<java module>"), Collections.<ImportPath>emptyList(), PlatformToKotlinClassMap.EMPTY)
            );
        }
        return subModule;
    }

    @Nullable
    public ClassDescriptor resolveClass(@NotNull FqName qualifiedName, @NotNull DescriptorSearchRule searchRule) {
        if (LAZY) {
            return getSubModule().getClass(qualifiedName);
        }
        return classResolver.resolveClass(qualifiedName, searchRule);
    }

    @Override
    public ClassDescriptor resolveClass(@NotNull FqName qualifiedName) {
        return resolveClass(qualifiedName, IGNORE_KOTLIN_SOURCES);
    }

    @NotNull
    public JavaPackageFragmentProvider getPackageFragmentProvider() {
        return packageFragmentProvider;
    }
}
