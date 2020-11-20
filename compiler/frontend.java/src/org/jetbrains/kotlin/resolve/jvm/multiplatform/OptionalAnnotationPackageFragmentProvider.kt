/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.multiplatform

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.CompilerDeserializationConfiguration
import org.jetbrains.kotlin.resolve.sam.SamConversionResolver
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.MemberScopeImpl
import org.jetbrains.kotlin.serialization.deserialization.*
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol
import org.jetbrains.kotlin.storage.NotNullLazyValue
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.storage.getValue
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.addIfNotNull

class OptionalAnnotationPackageFragmentProvider(
    module: ModuleDescriptor,
    storageManager: StorageManager,
    notFoundClasses: NotFoundClasses,
    languageVersionSettings: LanguageVersionSettings,
    packagePartProvider: PackagePartProvider,
) : PackageFragmentProviderOptimized {
    val packages: Map<FqName, PackageFragmentDescriptor> by storageManager.createLazyValue p@{
        // We call getAllOptionalAnnotationClasses under lazy value only because IncrementalPackagePartProvider requires
        // deserializationConfiguration to be injected.
        val optionalAnnotationClasses = packagePartProvider.getAllOptionalAnnotationClasses()
        if (optionalAnnotationClasses.isEmpty()) return@p emptyMap()

        mutableMapOf<FqName, PackageFragmentDescriptor>().also { packages ->
            // We use BuiltInSerializerProtocol when serializing optional annotation classes (see
            // JvmOptionalAnnotationSerializerExtension). Use it in deserialization as well, to be able to load annotations on
            // optional annotation classes and their members (in particular, the `@OptionalExpectation` annotation).
            val serializerProtocol = BuiltInSerializerProtocol

            val classDataFinder = OptionalAnnotationClassDataFinder(optionalAnnotationClasses)
            val components = storageManager.createLazyValue {
                DeserializationComponents(
                    storageManager, module, CompilerDeserializationConfiguration(languageVersionSettings),
                    classDataFinder,
                    AnnotationAndConstantLoaderImpl(module, notFoundClasses, serializerProtocol),
                    this,
                    LocalClassifierTypeSettings.Default,
                    ErrorReporter.DO_NOTHING,
                    LookupTracker.DO_NOTHING,
                    FlexibleTypeDeserializer.ThrowException,
                    emptyList(),
                    notFoundClasses,
                    ContractDeserializer.DEFAULT,
                    extensionRegistryLite = serializerProtocol.extensionRegistry,
                    samConversionResolver = SamConversionResolver.Empty
                )
            }

            for ((packageFqName, classes) in classDataFinder.classIdToData.entries.groupBy { it.key.packageFqName }) {
                val classNames = classes.mapNotNull { (classId) ->
                    classId.shortClassName.takeUnless { classId.isNestedClass }
                }.toSet()
                // TODO: make this lazy value more granular, e.g. a memoized function ClassId -> ClassDescriptor
                val classDescriptors = storageManager.createLazyValue {
                    classes.mapNotNull { (classId, classData) ->
                        components().classDeserializer.deserializeClass(classId, classData)
                    }.associateBy(ClassDescriptor::getName)
                }
                packages[packageFqName] = PackageFragmentForOptionalAnnotations(module, packageFqName, classNames, classDescriptors)
            }
        }
    }

    override fun collectPackageFragments(fqName: FqName, packageFragments: MutableCollection<PackageFragmentDescriptor>) =
        packageFragments.addIfNotNull(packages[fqName])

    override fun getPackageFragments(fqName: FqName): List<PackageFragmentDescriptor> =
        packages[fqName]?.let(::listOf).orEmpty()

    override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<FqName> =
        emptyList()
}

private class OptionalAnnotationClassDataFinder(classes: List<ClassData>) : ClassDataFinder {
    val classIdToData = classes.associateBy { (nameResolver, klass) -> nameResolver.getClassId(klass.fqName) }

    override fun findClassData(classId: ClassId): ClassData? = classIdToData[classId]
}

private class PackageFragmentForOptionalAnnotations(
    module: ModuleDescriptor,
    fqName: FqName,
    classNames: Set<Name>,
    classDescriptors: NotNullLazyValue<Map<Name, ClassDescriptor>>,
) : PackageFragmentDescriptorImpl(module, fqName) {
    private val scope = object : MemberScopeImpl() {
        override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? = classDescriptors()[name]

        override fun getContributedDescriptors(
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean
        ): Collection<DeclarationDescriptor> =
            if (kindFilter.acceptsKinds(DescriptorKindFilter.CLASSIFIERS_MASK)) classDescriptors().values else emptyList()

        override fun getClassifierNames(): Set<Name> = classNames

        override fun printScopeStructure(p: Printer) {
            p.print("PackageFragmentForOptionalAnnotations{${classNames.joinToString(transform = Name::asString)}}")
        }
    }

    override fun getMemberScope(): MemberScope = scope
}
