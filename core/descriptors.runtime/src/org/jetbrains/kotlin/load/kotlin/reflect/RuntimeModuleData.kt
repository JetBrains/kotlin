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

package org.jetbrains.kotlin.load.kotlin.reflect

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.ReflectionTypes
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.ModuleParameters
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.load.java.components.*
import org.jetbrains.kotlin.load.java.lazy.JavaResolverComponents
import org.jetbrains.kotlin.load.java.lazy.LazyJavaPackageFragmentProvider
import org.jetbrains.kotlin.load.java.lazy.PackageMappingProvider
import org.jetbrains.kotlin.load.java.lazy.SingleModuleClassResolver
import org.jetbrains.kotlin.load.java.reflect.ReflectJavaClassFinder
import org.jetbrains.kotlin.load.java.structure.JavaPropertyInitializerEvaluator
import org.jetbrains.kotlin.load.kotlin.BinaryClassAnnotationAndConstantLoaderImpl
import org.jetbrains.kotlin.load.kotlin.DeserializationComponentsForJava
import org.jetbrains.kotlin.load.kotlin.DeserializedDescriptorResolver
import org.jetbrains.kotlin.load.kotlin.JavaClassDataFinder
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap
import org.jetbrains.kotlin.resolve.jvm.JavaDescriptorResolver
import org.jetbrains.kotlin.serialization.deserialization.LocalClassResolver
import org.jetbrains.kotlin.storage.LockBasedStorageManager

public class RuntimeModuleData private constructor(public val module: ModuleDescriptor, public val localClassResolver: LocalClassResolver) {
    companion object {
        public fun create(classLoader: ClassLoader): RuntimeModuleData {
            val storageManager = LockBasedStorageManager()
            val module = ModuleDescriptorImpl(Name.special("<runtime module for $classLoader>"), storageManager,
                                              ModuleParameters(listOf(), JavaToKotlinClassMap.INSTANCE))

            val reflectKotlinClassFinder = ReflectKotlinClassFinder(classLoader)
            val deserializedDescriptorResolver = DeserializedDescriptorResolver(RuntimeErrorReporter)
            val singleModuleClassResolver = SingleModuleClassResolver()
            val globalJavaResolverContext = JavaResolverComponents(
                    storageManager, ReflectJavaClassFinder(classLoader), reflectKotlinClassFinder, deserializedDescriptorResolver,
                    ExternalAnnotationResolver.EMPTY, ExternalSignatureResolver.DO_NOTHING, RuntimeErrorReporter, JavaResolverCache.EMPTY,
                    JavaPropertyInitializerEvaluator.DoNothing, SamConversionResolver, RuntimeSourceElementFactory, singleModuleClassResolver
            )
            val lazyJavaPackageFragmentProvider =
                    LazyJavaPackageFragmentProvider(globalJavaResolverContext, module, ReflectionTypes(module), PackageMappingProvider.EMPTY)
            val javaDescriptorResolver = JavaDescriptorResolver(lazyJavaPackageFragmentProvider, module)
            val javaClassDataFinder = JavaClassDataFinder(reflectKotlinClassFinder, deserializedDescriptorResolver)
            val binaryClassAnnotationAndConstantLoader = BinaryClassAnnotationAndConstantLoaderImpl(module, storageManager, reflectKotlinClassFinder, RuntimeErrorReporter)
            val deserializationComponentsForJava = DeserializationComponentsForJava(storageManager, module, javaClassDataFinder, binaryClassAnnotationAndConstantLoader, lazyJavaPackageFragmentProvider)
            singleModuleClassResolver.resolver = javaDescriptorResolver
            deserializedDescriptorResolver.setComponents(deserializationComponentsForJava)

            module.setDependencies(module, KotlinBuiltIns.getInstance().getBuiltInsModule())
            module.initialize(javaDescriptorResolver.packageFragmentProvider)

            return RuntimeModuleData(module, deserializationComponentsForJava.components.localClassResolver)
        }
    }
}
