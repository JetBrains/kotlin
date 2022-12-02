/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.load.kotlin

import org.jetbrains.kotlin.builtins.ReflectionTypes
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltIns
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltInsPackageFragmentProvider
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.NotFoundClasses
import org.jetbrains.kotlin.descriptors.SupertypeLoopChecker
import org.jetbrains.kotlin.descriptors.deserialization.AdditionalClassPartsProvider
import org.jetbrains.kotlin.descriptors.deserialization.PlatformDependentDeclarationFilter
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.load.java.AnnotationTypeQualifierResolver
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.JavaClassesTracker
import org.jetbrains.kotlin.load.java.JavaModuleAnnotationsProvider
import org.jetbrains.kotlin.load.java.JavaTypeEnhancementState
import org.jetbrains.kotlin.load.java.components.JavaPropertyInitializerEvaluator
import org.jetbrains.kotlin.load.java.components.JavaResolverCache
import org.jetbrains.kotlin.load.java.components.SignaturePropagator
import org.jetbrains.kotlin.load.java.lazy.*
import org.jetbrains.kotlin.load.java.sources.JavaSourceElementFactory
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.load.java.typeEnhancement.JavaTypeEnhancement
import org.jetbrains.kotlin.load.java.typeEnhancement.SignatureEnhancement
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.jvm.JavaDescriptorResolver
import org.jetbrains.kotlin.resolve.sam.SamConversionResolverImpl
import org.jetbrains.kotlin.serialization.deserialization.*
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.DefaultTypeAttributeTranslator
import org.jetbrains.kotlin.types.checker.NewKotlinTypeChecker
import org.jetbrains.kotlin.types.extensions.TypeAttributeTranslators

// This class is needed only for easier injection: exact types of needed components are specified in the constructor here.
// Otherwise injector generator is not smart enough to deduce, for example, which package fragment provider DeserializationComponents needs
class DeserializationComponentsForJava(
    storageManager: StorageManager,
    moduleDescriptor: ModuleDescriptor,
    configuration: DeserializationConfiguration,
    classDataFinder: JavaClassDataFinder,
    annotationAndConstantLoader: BinaryClassAnnotationAndConstantLoaderImpl,
    packageFragmentProvider: LazyJavaPackageFragmentProvider,
    notFoundClasses: NotFoundClasses,
    errorReporter: ErrorReporter,
    lookupTracker: LookupTracker,
    contractDeserializer: ContractDeserializer,
    kotlinTypeChecker: NewKotlinTypeChecker,
    typeAttributeTranslators: TypeAttributeTranslators
) {
    val components: DeserializationComponents

    init {
        // currently built-ins may be not an instance of JvmBuiltIns only in case of built-ins serialization
        val jvmBuiltIns = moduleDescriptor.builtIns as? JvmBuiltIns
        components = DeserializationComponents(
            storageManager, moduleDescriptor, configuration, classDataFinder, annotationAndConstantLoader, packageFragmentProvider,
            LocalClassifierTypeSettings.Default, errorReporter, lookupTracker, JavaFlexibleTypeDeserializer,
            emptyList(), notFoundClasses, contractDeserializer,
            additionalClassPartsProvider = jvmBuiltIns?.customizer ?: AdditionalClassPartsProvider.None,
            platformDependentDeclarationFilter = jvmBuiltIns?.customizer ?: PlatformDependentDeclarationFilter.NoPlatformDependent,
            extensionRegistryLite = JvmProtoBufUtil.EXTENSION_REGISTRY,
            kotlinTypeChecker = kotlinTypeChecker, samConversionResolver = SamConversionResolverImpl(storageManager, emptyList()),
            typeAttributeTranslators = typeAttributeTranslators.translators
        )
    }

    companion object {

        /** Contains [DeserializationComponentsForJava] and some related information. */
        class ModuleData(
            val deserializationComponentsForJava: DeserializationComponentsForJava,
            val deserializedDescriptorResolver: DeserializedDescriptorResolver
        )

        fun createModuleData(
            kotlinClassFinder: KotlinClassFinder,
            jvmBuiltInsKotlinClassFinder: KotlinClassFinder,
            javaClassFinder: JavaClassFinder,
            moduleName: String,
            errorReporter: ErrorReporter,
            javaSourceElementFactory: JavaSourceElementFactory
        ): ModuleData {
            val storageManager = LockBasedStorageManager("DeserializationComponentsForJava.ModuleData")
            val builtIns = JvmBuiltIns(storageManager, JvmBuiltIns.Kind.FROM_DEPENDENCIES)
            val module = ModuleDescriptorImpl(Name.special("<$moduleName>"), storageManager, builtIns)
            builtIns.builtInsModule = module

            builtIns.initialize(module, isAdditionalBuiltInsFeatureSupported = true)

            val deserializedDescriptorResolver = DeserializedDescriptorResolver()
            val singleModuleClassResolver = SingleModuleClassResolver()
            val notFoundClasses = NotFoundClasses(storageManager, module)

            val lazyJavaPackageFragmentProvider =
                makeLazyJavaPackageFragmentProvider(
                    javaClassFinder, module, storageManager, notFoundClasses,
                    kotlinClassFinder, deserializedDescriptorResolver,
                    errorReporter, javaSourceElementFactory, singleModuleClassResolver
                )

            val deserializationComponentsForJava =
                makeDeserializationComponentsForJava(
                    module, storageManager, notFoundClasses, lazyJavaPackageFragmentProvider,
                    kotlinClassFinder, deserializedDescriptorResolver, errorReporter, JvmMetadataVersion.INSTANCE
                )

            deserializedDescriptorResolver.setComponents(deserializationComponentsForJava)

            val javaDescriptorResolver = JavaDescriptorResolver(lazyJavaPackageFragmentProvider, JavaResolverCache.EMPTY)
            singleModuleClassResolver.resolver = javaDescriptorResolver

            val builtinsProvider = JvmBuiltInsPackageFragmentProvider(
                storageManager, jvmBuiltInsKotlinClassFinder, module, notFoundClasses, builtIns.customizer, builtIns.customizer,
                DeserializationConfiguration.Default, NewKotlinTypeChecker.Default, SamConversionResolverImpl(storageManager, emptyList())
            )

            module.setDependencies(module)
            module.initialize(
                CompositePackageFragmentProvider(
                    listOf(javaDescriptorResolver.packageFragmentProvider, builtinsProvider),
                    "CompositeProvider@RuntimeModuleData for $module"
                )
            )

            return ModuleData(deserializationComponentsForJava, deserializedDescriptorResolver)
        }
    }
}

fun makeLazyJavaPackageFragmentProvider(
    javaClassFinder: JavaClassFinder,
    module: ModuleDescriptor,
    storageManager: StorageManager,
    notFoundClasses: NotFoundClasses,
    reflectKotlinClassFinder: KotlinClassFinder,
    deserializedDescriptorResolver: DeserializedDescriptorResolver,
    errorReporter: ErrorReporter,
    javaSourceElementFactory: JavaSourceElementFactory,
    singleModuleClassResolver: ModuleClassResolver,
    packagePartProvider: PackagePartProvider = PackagePartProvider.Empty
): LazyJavaPackageFragmentProvider {
    val javaResolverComponents = JavaResolverComponents(
        storageManager, javaClassFinder, reflectKotlinClassFinder, deserializedDescriptorResolver,
        SignaturePropagator.DO_NOTHING, errorReporter, JavaResolverCache.EMPTY,
        JavaPropertyInitializerEvaluator.DoNothing, SamConversionResolverImpl(storageManager, emptyList()), javaSourceElementFactory,
        singleModuleClassResolver, packagePartProvider, SupertypeLoopChecker.EMPTY, LookupTracker.DO_NOTHING, module,
        ReflectionTypes(module, notFoundClasses), AnnotationTypeQualifierResolver(JavaTypeEnhancementState.DEFAULT),
        SignatureEnhancement(JavaTypeEnhancement(JavaResolverSettings.Default)),
        JavaClassesTracker.Default, JavaResolverSettings.Default, NewKotlinTypeChecker.Default, JavaTypeEnhancementState.DEFAULT,
        object : JavaModuleAnnotationsProvider {
            override fun getAnnotationsForModuleOwnerOfClass(classId: ClassId): List<JavaAnnotation>? = null
        }
    )

    return LazyJavaPackageFragmentProvider(javaResolverComponents)
}

fun makeDeserializationComponentsForJava(
    module: ModuleDescriptor,
    storageManager: StorageManager,
    notFoundClasses: NotFoundClasses,
    lazyJavaPackageFragmentProvider: LazyJavaPackageFragmentProvider,
    reflectKotlinClassFinder: KotlinClassFinder,
    deserializedDescriptorResolver: DeserializedDescriptorResolver,
    errorReporter: ErrorReporter,
    jvmMetadataVersion: JvmMetadataVersion
): DeserializationComponentsForJava {
    val javaClassDataFinder = JavaClassDataFinder(reflectKotlinClassFinder, deserializedDescriptorResolver)
    val binaryClassAnnotationAndConstantLoader = createBinaryClassAnnotationAndConstantLoader(
        module, notFoundClasses, storageManager, reflectKotlinClassFinder, jvmMetadataVersion
    )
    return DeserializationComponentsForJava(
        storageManager, module, DeserializationConfiguration.Default, javaClassDataFinder,
        binaryClassAnnotationAndConstantLoader, lazyJavaPackageFragmentProvider, notFoundClasses,
        errorReporter, LookupTracker.DO_NOTHING, ContractDeserializer.DEFAULT, NewKotlinTypeChecker.Default,
        TypeAttributeTranslators(listOf(DefaultTypeAttributeTranslator))
    )
}
