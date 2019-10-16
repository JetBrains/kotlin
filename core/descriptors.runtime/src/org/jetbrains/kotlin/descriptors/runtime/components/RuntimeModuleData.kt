/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.descriptors.runtime.components

import org.jetbrains.kotlin.builtins.ReflectionTypes
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltIns
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltInsPackageFragmentProvider
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.NotFoundClasses
import org.jetbrains.kotlin.descriptors.SupertypeLoopChecker
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.load.java.AnnotationTypeQualifierResolver
import org.jetbrains.kotlin.load.java.JavaClassesTracker
import org.jetbrains.kotlin.load.java.components.JavaPropertyInitializerEvaluator
import org.jetbrains.kotlin.load.java.components.JavaResolverCache
import org.jetbrains.kotlin.load.java.components.SamConversionResolver
import org.jetbrains.kotlin.load.java.components.SignaturePropagator
import org.jetbrains.kotlin.load.java.lazy.*
import org.jetbrains.kotlin.load.java.typeEnhancement.SignatureEnhancement
import org.jetbrains.kotlin.load.kotlin.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.jvm.JavaDescriptorResolver
import org.jetbrains.kotlin.serialization.deserialization.ContractDeserializer
import org.jetbrains.kotlin.serialization.deserialization.DeserializationComponents
import org.jetbrains.kotlin.serialization.deserialization.DeserializationConfiguration
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.checker.NewKotlinTypeChecker
import org.jetbrains.kotlin.utils.Jsr305State

class RuntimeModuleData private constructor(
    val deserialization: DeserializationComponents,
    val packagePartScopeCache: PackagePartScopeCache
) {
    val module: ModuleDescriptor get() = deserialization.moduleDescriptor

    companion object {
        fun create(classLoader: ClassLoader): RuntimeModuleData {
            val storageManager = LockBasedStorageManager("RuntimeModuleData")
            val builtIns = JvmBuiltIns(storageManager, JvmBuiltIns.Kind.FROM_DEPENDENCIES)
            val module = ModuleDescriptorImpl(Name.special("<runtime module for $classLoader>"), storageManager, builtIns)
            builtIns.builtInsModule = module

            builtIns.initialize(module, isAdditionalBuiltInsFeatureSupported = true)

            val reflectKotlinClassFinder = ReflectKotlinClassFinder(classLoader)
            val deserializedDescriptorResolver = DeserializedDescriptorResolver()
            val singleModuleClassResolver = SingleModuleClassResolver()
            val notFoundClasses = NotFoundClasses(storageManager, module)

            val lazyJavaPackageFragmentProvider =
                makeLazyJavaPackageFragmentFromClassLoaderProvider(
                    classLoader, module, storageManager, notFoundClasses,
                    reflectKotlinClassFinder, deserializedDescriptorResolver, singleModuleClassResolver
                )

            val deserializationComponentsForJava =
                makeDeserializationComponentsForJava(
                    module, storageManager, notFoundClasses, lazyJavaPackageFragmentProvider,
                    reflectKotlinClassFinder, deserializedDescriptorResolver
                )

            deserializedDescriptorResolver.setComponents(deserializationComponentsForJava)

            val javaDescriptorResolver = JavaDescriptorResolver(lazyJavaPackageFragmentProvider, JavaResolverCache.EMPTY)
            singleModuleClassResolver.resolver = javaDescriptorResolver

            // .kotlin_builtins files should be found by the same class loader that loaded stdlib classes
            val stdlibClassLoader = Unit::class.java.classLoader
            val builtinsProvider = JvmBuiltInsPackageFragmentProvider(
                storageManager, ReflectKotlinClassFinder(stdlibClassLoader), module, notFoundClasses, builtIns.settings, builtIns.settings,
                DeserializationConfiguration.Default, NewKotlinTypeChecker.Default
            )

            module.setDependencies(module)
            module.initialize(CompositePackageFragmentProvider(listOf(javaDescriptorResolver.packageFragmentProvider, builtinsProvider)))

            return RuntimeModuleData(
                deserializationComponentsForJava.components,
                PackagePartScopeCache(deserializedDescriptorResolver, reflectKotlinClassFinder)
            )
        }
    }
}

fun makeLazyJavaPackageFragmentFromClassLoaderProvider(
    classLoader: ClassLoader,
    module: ModuleDescriptor,
    storageManager: StorageManager,
    notFoundClasses: NotFoundClasses,
    reflectKotlinClassFinder: KotlinClassFinder,
    deserializedDescriptorResolver: DeserializedDescriptorResolver,
    singleModuleClassResolver: ModuleClassResolver,
    packagePartProvider: PackagePartProvider = PackagePartProvider.Empty
): LazyJavaPackageFragmentProvider {
    val annotationTypeQualifierResolver = AnnotationTypeQualifierResolver(storageManager, Jsr305State.DISABLED)
    val javaResolverComponents = JavaResolverComponents(
        storageManager, ReflectJavaClassFinder(classLoader), reflectKotlinClassFinder, deserializedDescriptorResolver,
        SignaturePropagator.DO_NOTHING, RuntimeErrorReporter, JavaResolverCache.EMPTY,
        JavaPropertyInitializerEvaluator.DoNothing, SamConversionResolver.Empty, RuntimeSourceElementFactory,
        singleModuleClassResolver, packagePartProvider, SupertypeLoopChecker.EMPTY, LookupTracker.DO_NOTHING, module,
        ReflectionTypes(module, notFoundClasses), annotationTypeQualifierResolver,
        SignatureEnhancement(annotationTypeQualifierResolver, Jsr305State.DISABLED),
        JavaClassesTracker.Default, JavaResolverSettings.Default, NewKotlinTypeChecker.Default
    )

    return LazyJavaPackageFragmentProvider(javaResolverComponents)
}

fun makeDeserializationComponentsForJava(
    module: ModuleDescriptor,
    storageManager: StorageManager,
    notFoundClasses: NotFoundClasses,
    lazyJavaPackageFragmentProvider: LazyJavaPackageFragmentProvider,
    reflectKotlinClassFinder: KotlinClassFinder,
    deserializedDescriptorResolver: DeserializedDescriptorResolver
): DeserializationComponentsForJava {
    val javaClassDataFinder = JavaClassDataFinder(reflectKotlinClassFinder, deserializedDescriptorResolver)
    val binaryClassAnnotationAndConstantLoader = BinaryClassAnnotationAndConstantLoaderImpl(
        module, notFoundClasses, storageManager, reflectKotlinClassFinder
    )
    return DeserializationComponentsForJava(
        storageManager, module, DeserializationConfiguration.Default, javaClassDataFinder,
        binaryClassAnnotationAndConstantLoader, lazyJavaPackageFragmentProvider, notFoundClasses,
        RuntimeErrorReporter, LookupTracker.DO_NOTHING, ContractDeserializer.DEFAULT, NewKotlinTypeChecker.Default
    )
}