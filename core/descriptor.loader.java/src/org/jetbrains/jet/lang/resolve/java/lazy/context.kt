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

import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.jet.lang.resolve.java.lazy.types.LazyJavaTypeResolver
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.jet.lang.resolve.java.structure.JavaTypeParameter
import org.jetbrains.jet.lang.resolve.java.JavaClassFinder
import org.jetbrains.jet.lang.resolve.java.resolver.*
import org.jetbrains.jet.lang.resolve.kotlin.DeserializedDescriptorResolver
import org.jetbrains.jet.lang.resolve.kotlin.KotlinClassFinder
import org.jetbrains.jet.lang.resolve.java.structure.JavaPropertyInitializerEvaluator
import org.jetbrains.jet.lang.resolve.java.sources.JavaSourceElementFactory
import org.jetbrains.jet.lang.resolve.java.structure.JavaTypeParameterListOwner

open class GlobalJavaResolverContext(
        val storageManager: StorageManager,
        val finder: JavaClassFinder,
        val kotlinClassFinder: KotlinClassFinder,
        val deserializedDescriptorResolver: DeserializedDescriptorResolver,
        val externalAnnotationResolver: ExternalAnnotationResolver,
        val externalSignatureResolver: ExternalSignatureResolver,
        val errorReporter: ErrorReporter,
        val methodSignatureChecker: MethodSignatureChecker,
        val javaResolverCache: JavaResolverCache,
        val javaPropertyInitializerEvaluator: JavaPropertyInitializerEvaluator,
        val samConversionResolver: SamConversionResolver,
        val sourceElementFactory: JavaSourceElementFactory,
        val moduleClassResolver: ModuleClassResolver
)

open class LazyJavaResolverContext(
        globalContext: GlobalJavaResolverContext,
        val packageFragmentProvider: LazyJavaPackageFragmentProvider,
        val javaClassResolver: LazyJavaClassResolver,
        val typeParameterResolver: TypeParameterResolver
) : GlobalJavaResolverContext(
        globalContext.storageManager,
        globalContext.finder,
        globalContext.kotlinClassFinder,
        globalContext.deserializedDescriptorResolver,
        globalContext.externalAnnotationResolver,
        globalContext.externalSignatureResolver,
        globalContext.errorReporter,
        globalContext.methodSignatureChecker,
        globalContext.javaResolverCache,
        globalContext.javaPropertyInitializerEvaluator,
        globalContext.samConversionResolver,
        globalContext.sourceElementFactory,
        globalContext.moduleClassResolver
) {
    val typeResolver = LazyJavaTypeResolver(this, typeParameterResolver)
}

fun LazyJavaResolverContext.child(
        typeParameterResolver: TypeParameterResolver
) = LazyJavaResolverContext(this, packageFragmentProvider, javaClassResolver, typeParameterResolver)


fun LazyJavaResolverContext.child(
        containingDeclaration: DeclarationDescriptor,
        typeParameterOwner: JavaTypeParameterListOwner
) = this.child(LazyJavaTypeParameterResolver(this, containingDeclaration, typeParameterOwner))
