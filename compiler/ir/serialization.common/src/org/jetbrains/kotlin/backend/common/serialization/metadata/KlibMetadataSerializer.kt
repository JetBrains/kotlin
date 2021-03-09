/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.metadata

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.backend.common.serialization.isExpectMember
import org.jetbrains.kotlin.backend.common.serialization.isSerializableExpectClass
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.serialization.ApproximatingStringTable
import org.jetbrains.kotlin.serialization.DescriptorSerializer
import org.jetbrains.kotlin.serialization.StringTableImpl
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPropertyDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedSimpleFunctionDescriptor

internal fun <T, R> Iterable<T>.maybeChunked(size: Int?, transform: (List<T>) -> R): List<R>
    = size?.let { this.chunked(size, transform) } ?: listOf(transform(this.toList()))

abstract class KlibMetadataSerializer(
    val languageVersionSettings: LanguageVersionSettings,
    val metadataVersion: BinaryVersion,
    val project: Project?,
    val exportKDoc: Boolean = false,
    val skipExpects: Boolean = false,
    val includeOnlyModuleContent: Boolean = false,
    private val allowErrorTypes: Boolean
) {

    lateinit var serializerContext: SerializerContext

    data class SerializerContext(
        val serializerExtension: KlibMetadataSerializerExtension,
        val topSerializer: DescriptorSerializer,
        var classSerializer: DescriptorSerializer = topSerializer
    )

    protected fun createNewContext(): SerializerContext {

        val extension = KlibMetadataSerializerExtension(
            languageVersionSettings,
            metadataVersion,
            ApproximatingStringTable(),
            allowErrorTypes,
            exportKDoc
        )
        return SerializerContext(
            extension,
            DescriptorSerializer.createTopLevel(extension)
        )
    }

    protected inline fun <T> withNewContext(crossinline block: SerializerContext.() -> T): T {
        serializerContext = createNewContext()
        return with(serializerContext, block)
    }


    private fun SerializerContext.buildFragment(
        packageProto: ProtoBuf.Package,
        classesProto: List<Pair<ProtoBuf.Class, Int>>,
        fqName: FqName,
        isEmpty: Boolean
    ): ProtoBuf.PackageFragment {

        val (stringTableProto, nameTableProto) = serializerExtension.stringTable.buildProto()

        return ProtoBuf.PackageFragment.newBuilder()
            .setPackage(packageProto)
            .addAllClass_(classesProto.map { it.first })
            .setStrings(stringTableProto)
            .setQualifiedNames(nameTableProto)
            .also { packageFragment ->
                classesProto.forEach {
                    packageFragment.addExtension(KlibMetadataProtoBuf.className, it.second )
                }
                packageFragment.setExtension(KlibMetadataProtoBuf.isEmpty, isEmpty)
                packageFragment.setExtension(KlibMetadataProtoBuf.fqName, fqName.asString())
            }
            .build()
    }

    private fun serializeClass(packageName: FqName,
                               classDescriptor: ClassDescriptor): List<Pair<ProtoBuf.Class, Int>> {
        with(serializerContext) {
            val previousSerializer = classSerializer

            classSerializer = DescriptorSerializer.create(classDescriptor, serializerExtension, classSerializer, project)
            val classProto = classSerializer.classProto(classDescriptor).build() ?: error("Class not serialized: $classDescriptor")
            //builder.addClass(classProto)

            val index = classSerializer.stringTable.getFqNameIndex(classDescriptor)
            //builder.addExtension(KlibMetadataProtoBuf.className, index)

            val classes = serializeClasses(
                packageName/*, builder*/,
                classDescriptor.unsubstitutedInnerClassesScope
                    .getContributedDescriptors(DescriptorKindFilter.CLASSIFIERS)
            )

            classSerializer = previousSerializer
            return classes + Pair(classProto, index)
        }
    }

    // TODO: we filter out expects with present actuals.
    // This is done because deserialized member scope doesn't give us actuals
    // when it has a choice
    private fun List<DeclarationDescriptor>.filterOutExpectsWithActuals(): List<DeclarationDescriptor> {
        val actualClassIds = this.filter{ !it.isExpectMember }.map { ClassId.topLevel(it.fqNameSafe) }
        return this.filterNot {
            // TODO: this only filters classes for now.
            // Need to do the same for functions etc
            (it is ClassDescriptor) && it.isExpect() && ClassId.topLevel(it.fqNameSafe) in actualClassIds
        }
    }

    protected fun List<DeclarationDescriptor>.filterOutExpects(): List<DeclarationDescriptor> =
        if (skipExpects)
            this.filterNot { it.isExpectMember && !it.isSerializableExpectClass }
        else
            this.filterOutExpectsWithActuals()

    private fun serializeClasses(packageName: FqName,
                                 //builder: ProtoBuf.PackageFragment.Builder,
                                 descriptors: Collection<DeclarationDescriptor>): List<Pair<ProtoBuf.Class, Int>> {

        return descriptors.filterIsInstance<ClassDescriptor>().flatMap {
            serializeClass(packageName, it)
        }
    }

    private fun emptyPackageProto(): ProtoBuf.Package = ProtoBuf.Package.newBuilder().build()

    private fun SerializerContext.buildPackageProto(
        fqName: FqName,
        descriptors: List<DeclarationDescriptor>) = topSerializer.packagePartProto(fqName, descriptors).build()
        ?: error("Package fragments not serialized: for $descriptors")

    protected fun serializeDescriptors(
        fqName: FqName,
        allClassifierDescriptors: List<DeclarationDescriptor>,
        allTopLevelDescriptors: List<DeclarationDescriptor>
    ): List<ProtoBuf.PackageFragment> {

        val classifierDescriptors = allClassifierDescriptors.filterOutExpects()
        val topLevelDescriptors = allTopLevelDescriptors.filterOutExpects()

        if (TOP_LEVEL_CLASS_DECLARATION_COUNT_PER_FILE == null &&
            TOP_LEVEL_DECLARATION_COUNT_PER_FILE == null) {

            val typeAliases = classifierDescriptors.filterIsInstance<TypeAliasDescriptor>()
            val nonCassDescriptors = topLevelDescriptors+typeAliases


            return listOf(withNewContext {
                    val packageProto = if (nonCassDescriptors.isEmpty())
                        emptyPackageProto()
                    else
                        buildPackageProto(fqName, nonCassDescriptors)

                    buildFragment(
                        packageProto,
                        serializeClasses(fqName, classifierDescriptors),
                        fqName,
                        topLevelDescriptors.isEmpty() && classifierDescriptors.isEmpty()
                    )
                }
            )
        }

        val result = mutableListOf<ProtoBuf.PackageFragment>()

        result += classifierDescriptors.maybeChunked(TOP_LEVEL_CLASS_DECLARATION_COUNT_PER_FILE) { descriptors ->

            withNewContext {

                //val classesProto = buildClassesProto { classesBuilder ->
                //    serializeClasses(fqName, classesBuilder, descriptors)
                //}
                val classesProto = serializeClasses(fqName, descriptors)

                val typeAliases = descriptors.filterIsInstance<TypeAliasDescriptor>()
                val packageProto =
                    if (typeAliases.isNotEmpty()) buildPackageProto(fqName, typeAliases)
                    else emptyPackageProto()

                buildFragment(
                    packageProto,
                    classesProto,
                    fqName,
                    descriptors.isEmpty()
                )
            }
        }

        result += topLevelDescriptors.maybeChunked(TOP_LEVEL_DECLARATION_COUNT_PER_FILE) { descriptors ->
            withNewContext {
                buildFragment(
                    buildPackageProto(fqName, descriptors),
                    emptyList(),
                    fqName,
                    descriptors.isEmpty()
                )
            }
        }

        if (result.isEmpty()) {
            result += withNewContext {
                buildFragment(
                    emptyPackageProto(),
                    emptyList(),
                    fqName,
                    true
                )
            }
        }

        return result
    }

    protected fun getPackagesFqNames(module: ModuleDescriptor): Set<FqName> {
        val result = mutableSetOf<FqName>()

        fun getSubPackagesOfModule(fqName: FqName) =
            if (includeOnlyModuleContent) {
                module.packageFragmentProviderForModuleContentWithoutDependencies.getSubPackagesOf(fqName) { true }
            } else {
                module.getSubPackagesOf(fqName) { true }
            }

        fun getSubPackages(fqName: FqName) {
            result.add(fqName)
            getSubPackagesOfModule(fqName).forEach { getSubPackages(it) }
        }

        getSubPackages(FqName.ROOT)
        return result
    }

    fun serializeHeader(
        moduleDescriptor: ModuleDescriptor,
        fragmentNames: List<String>,
        emptyPackages: List<String>
    ): KlibMetadataProtoBuf.Header {
        val header = KlibMetadataProtoBuf.Header.newBuilder()

        header.moduleName = moduleDescriptor.name.asString()

        if (languageVersionSettings.isPreRelease()) {
            header.flags = 1
        }

        val experimentalAnnotationFqNames = languageVersionSettings.getFlag(AnalysisFlags.experimental)
        if (experimentalAnnotationFqNames.isNotEmpty()) {
            val stringTable = StringTableImpl()
            for (fqName in experimentalAnnotationFqNames) {
                val descriptor = moduleDescriptor.resolveClassByFqName(FqName(fqName), NoLookupLocation.FOR_ALREADY_TRACKED) ?: continue
                header.addAnnotation(ProtoBuf.Annotation.newBuilder().apply {
                    id = stringTable.getFqNameIndex(descriptor)
                })
            }
            val (strings, qualifiedNames) = stringTable.buildProto()
            header.strings = strings
            header.qualifiedNames = qualifiedNames
        }
        fragmentNames.forEach {
            header.addPackageFragmentName(it)
        }
        emptyPackages.forEach {
            header.addEmptyPackage(it)
        }

        return header.build()
    }

    // For platform libraries we get HUGE files.
    // Indexing them in IDEA takes ages.
    // So we split them into chunks.
    abstract protected val TOP_LEVEL_DECLARATION_COUNT_PER_FILE: Int?
    abstract protected val TOP_LEVEL_CLASS_DECLARATION_COUNT_PER_FILE: Int?
}

fun DeclarationDescriptor.extractFileId(): Int? = when (this) {
    is DeserializedClassDescriptor -> classProto.getExtension(KlibMetadataProtoBuf.classFile)
    is DeserializedSimpleFunctionDescriptor -> proto.getExtension(KlibMetadataProtoBuf.functionFile)
    is DeserializedPropertyDescriptor -> proto.getExtension(KlibMetadataProtoBuf.propertyFile)
    else -> null
}

internal val ModuleDescriptor.packageFragmentProviderForModuleContentWithoutDependencies: PackageFragmentProvider
    get() = (this as? ModuleDescriptorImpl)?.packageFragmentProviderForModuleContentWithoutDependencies
        ?: error("Can't get a module content package fragments, it's not a ${ModuleDescriptorImpl::class.simpleName}.")
