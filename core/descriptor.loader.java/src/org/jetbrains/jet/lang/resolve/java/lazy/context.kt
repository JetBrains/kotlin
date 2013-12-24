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

package org.jetbrains.jet.lang.resolve.java.lazy

import org.jetbrains.jet.storage.StorageManager
import org.jetbrains.jet.lang.resolve.java.lazy.types.LazyJavaTypeResolver
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.lang.resolve.java.structure.JavaTypeParameter
import org.jetbrains.jet.lang.resolve.java.JavaClassFinder
import org.jetbrains.jet.lang.resolve.java.resolver.ExternalAnnotationResolver
import org.jetbrains.jet.lang.resolve.java.resolver.ExternalSignatureResolver
import org.jetbrains.jet.lang.resolve.java.resolver.MethodSignatureChecker
import org.jetbrains.jet.lang.resolve.java.resolver.ErrorReporter
import org.jetbrains.jet.lang.resolve.java.resolver.JavaResolverCache
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver

open class GlobalJavaResolverContext(
        val storageManager: StorageManager,
        val finder: JavaClassFinder,
        val javaClassResolver: LazyJavaClassResolver,
        val externalAnnotationResolver: ExternalAnnotationResolver,
        val externalSignatureResolver: ExternalSignatureResolver,
        val errorReporter: ErrorReporter,
        val methodSignatureChecker: MethodSignatureChecker,
        val javaResolverCache: JavaResolverCache,
        val javaDescriptorResolver: JavaDescriptorResolver
)

open class LazyJavaResolverContext(
        val packageFragmentProvider: LazyJavaPackageFragmentProvider,
        storageManager: StorageManager,
        finder: JavaClassFinder,
        javaClassResolver: LazyJavaClassResolver,
        externalAnnotationResolver: ExternalAnnotationResolver,
        externalSignatureResolver: ExternalSignatureResolver,
        errorReporter: ErrorReporter,
        methodSignatureChecker: MethodSignatureChecker,
        javaResolverCache: JavaResolverCache,
        javaDescriptorResolver: JavaDescriptorResolver
) : GlobalJavaResolverContext(storageManager, finder, javaClassResolver,
                              externalAnnotationResolver, externalSignatureResolver,
                              errorReporter, methodSignatureChecker, javaResolverCache,
                              javaDescriptorResolver)

fun LazyJavaResolverContext.withTypes(
        typeParameterResolver: TypeParameterResolver = TypeParameterResolver.EMPTY
)  =  LazyJavaResolverContextWithTypes(
        packageFragmentProvider,
        storageManager,
        finder,
        javaClassResolver,
        externalAnnotationResolver,
        externalSignatureResolver,
        errorReporter,
        methodSignatureChecker,
        javaResolverCache,
        javaDescriptorResolver,
        LazyJavaTypeResolver(this, typeParameterResolver),
        typeParameterResolver)

class LazyJavaResolverContextWithTypes(
        packageFragmentProvider: LazyJavaPackageFragmentProvider,
        storageManager: StorageManager,
        finder: JavaClassFinder,
        javaClassResolver: LazyJavaClassResolver,
        externalAnnotationResolver: ExternalAnnotationResolver,
        externalSignatureResolver: ExternalSignatureResolver,
        errorReporter: ErrorReporter,
        methodSignatureChecker: MethodSignatureChecker,
        javaResolverCache: JavaResolverCache,
        javaDescriptorResolver: JavaDescriptorResolver,
        val typeResolver: LazyJavaTypeResolver,
        val typeParameterResolver: TypeParameterResolver
) : LazyJavaResolverContext(packageFragmentProvider, storageManager, finder, javaClassResolver,
                            externalAnnotationResolver, externalSignatureResolver,
                            errorReporter, methodSignatureChecker, javaResolverCache,
                            javaDescriptorResolver)

fun LazyJavaResolverContextWithTypes.child(
        containingDeclaration: DeclarationDescriptor,
        typeParameters: Set<JavaTypeParameter>
): LazyJavaResolverContextWithTypes = this.withTypes(LazyJavaTypeParameterResolver(this, containingDeclaration, typeParameters))