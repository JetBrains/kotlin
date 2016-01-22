/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.load.java.lazy

import org.jetbrains.kotlin.builtins.ReflectionTypes
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackagePartProvider
import org.jetbrains.kotlin.descriptors.SupertypeLoopChecker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.components.ExternalAnnotationResolver
import org.jetbrains.kotlin.load.java.components.SignaturePropagator
import org.jetbrains.kotlin.load.java.components.JavaResolverCache
import org.jetbrains.kotlin.load.java.components.SamConversionResolver
import org.jetbrains.kotlin.load.java.lazy.types.LazyJavaTypeResolver
import org.jetbrains.kotlin.load.java.sources.JavaSourceElementFactory
import org.jetbrains.kotlin.load.java.structure.JavaPropertyInitializerEvaluator
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameterListOwner
import org.jetbrains.kotlin.load.kotlin.DeserializedDescriptorResolver
import org.jetbrains.kotlin.load.kotlin.KotlinClassFinder
import org.jetbrains.kotlin.serialization.deserialization.ErrorReporter
import org.jetbrains.kotlin.storage.StorageManager

class JavaResolverComponents(
        val storageManager: StorageManager,
        val finder: JavaClassFinder,
        val kotlinClassFinder: KotlinClassFinder,
        val deserializedDescriptorResolver: DeserializedDescriptorResolver,
        val externalAnnotationResolver: ExternalAnnotationResolver,
        val signaturePropagator: SignaturePropagator,
        val errorReporter: ErrorReporter,
        val javaResolverCache: JavaResolverCache,
        val javaPropertyInitializerEvaluator: JavaPropertyInitializerEvaluator,
        val samConversionResolver: SamConversionResolver,
        val sourceElementFactory: JavaSourceElementFactory,
        val moduleClassResolver: ModuleClassResolver,
        val packageMapper: PackagePartProvider,
        val supertypeLoopChecker: SupertypeLoopChecker,
        val lookupTracker: LookupTracker
)

open class LazyJavaResolverContext(
        val components: JavaResolverComponents,
        val packageFragmentProvider: LazyJavaPackageFragmentProvider,
        val javaClassResolver: LazyJavaClassResolver,
        val module: ModuleDescriptor,
        val reflectionTypes: ReflectionTypes,
        val typeParameterResolver: TypeParameterResolver
) {
    val typeResolver = LazyJavaTypeResolver(this, typeParameterResolver)

    val storageManager: StorageManager
        get() = components.storageManager
}

fun LazyJavaResolverContext.child(
        typeParameterResolver: TypeParameterResolver
) = LazyJavaResolverContext(components, packageFragmentProvider, javaClassResolver, module, reflectionTypes, typeParameterResolver)


fun LazyJavaResolverContext.child(
        containingDeclaration: DeclarationDescriptor,
        typeParameterOwner: JavaTypeParameterListOwner,
        typeParametersIndexOffset: Int = 0
) = this.child(LazyJavaTypeParameterResolver(this, containingDeclaration, typeParameterOwner, typeParametersIndexOffset))
