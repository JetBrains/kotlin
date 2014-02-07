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
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor;
import org.jetbrains.jet.lang.resolve.java.lazy.GlobalJavaResolverContext;
import org.jetbrains.jet.lang.resolve.java.lazy.LazyJavaClassResolverWithCache;
import org.jetbrains.jet.lang.resolve.java.lazy.LazyJavaPackageFragmentProvider;
import org.jetbrains.jet.lang.resolve.java.resolver.*;
import org.jetbrains.jet.lang.resolve.kotlin.DeserializedDescriptorResolver;
import org.jetbrains.jet.lang.resolve.kotlin.KotlinClassFinder;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.DependencyClassByQualifiedNameResolver;
import org.jetbrains.jet.storage.StorageManager;

public class JavaDescriptorResolver implements DependencyClassByQualifiedNameResolver {
    public static final Name JAVA_ROOT = Name.special("<java_root>");

    private final ModuleDescriptor module;
    private final LazyJavaPackageFragmentProvider lazyJavaPackageFragmentProvider;

    public JavaDescriptorResolver(
            StorageManager storageManager,
            JavaClassFinder javaClassFinder,
            ExternalAnnotationResolver externalAnnotationResolver,
            ExternalSignatureResolver externalSignatureResolver,
            ErrorReporter errorReporter,
            MethodSignatureChecker signatureChecker,
            JavaResolverCache javaResolverCache,
            DeserializedDescriptorResolver deserializedDescriptorResolver,
            KotlinClassFinder kotlinClassFinder,
            ModuleDescriptor module
    ) {
        this.module = module;
        this.lazyJavaPackageFragmentProvider = new LazyJavaPackageFragmentProvider(
                new GlobalJavaResolverContext(
                        storageManager,
                        javaClassFinder,
                        kotlinClassFinder,
                        deserializedDescriptorResolver,
                        new LazyJavaClassResolverWithCache(javaResolverCache),
                        externalAnnotationResolver,
                        externalSignatureResolver,
                        errorReporter,
                        signatureChecker,
                        javaResolverCache,
                        this
                ),
                module
        );
    }

    @NotNull
    public ModuleDescriptor getModule() {
        return module;
    }

    @NotNull
    public LazyJavaPackageFragmentProvider getPackageFragmentProvider() {
        return lazyJavaPackageFragmentProvider;
    }

    @Nullable
    @Override
    public ClassDescriptor resolveClass(@NotNull FqName qualifiedName) {
        return getPackageFragmentProvider().getClass(qualifiedName);
    }

    @Nullable
    public PackageFragmentDescriptor getPackageFragment(@NotNull FqName fqName) {
        return getPackageFragmentProvider().getPackageFragment(fqName);
    }
}
