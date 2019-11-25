/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.metadata

import org.jetbrains.kotlin.backend.common.serialization.DescriptorTable
import org.jetbrains.kotlin.backend.common.serialization.newDescriptorUniqId
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
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
    val descriptorTable: DescriptorTable
) {

    lateinit var serializerContext: SerializerContext

    data class SerializerContext(
        val serializerExtension: KlibMetadataSerializerExtension,
        val topSerializer: DescriptorSerializer,
        var classSerializer: DescriptorSerializer = topSerializer
    )

    private fun declarationTableHandler(declarationDescriptor: DeclarationDescriptor): KlibMetadataProtoBuf.DescriptorUniqId {
        val index = descriptorTable.get(declarationDescriptor) ?: error("No descriptor ID found for $declarationDescriptor")
        return newDescriptorUniqId(index)
    }

    protected fun createNewContext(): SerializerContext {

        val extension = KlibMetadataSerializerExtension(
            languageVersionSettings,
            metadataVersion,
            ::declarationTableHandler,
            KlibMetadataStringTable()
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
        isEmpty: Boolean,
        bindingContext: BindingContext
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

            classSerializer = DescriptorSerializer.create(classDescriptor, serializerExtension, classSerializer)
            val classProto = classSerializer.classProto(classDescriptor).build() ?: error("Class not serialized: $classDescriptor")
            //builder.addClass(classProto)

            val index = classSerializer.stringTable.getFqNameIndex(classDescriptor)
            //builder.addExtension(KlibMetadataProtoBuf.className, index)

            val classes = serializeClasses(packageName/*, builder*/,
                classDescriptor.unsubstitutedInnerClassesScope
                    .getContributedDescriptors(DescriptorKindFilter.CLASSIFIERS))

            classSerializer = previousSerializer
            return classes + Pair(classProto, index)
        }
    }

    protected fun serializeClasses(packageName: FqName,
                                 //builder: ProtoBuf.PackageFragment.Builder,
                                 descriptors: Collection<DeclarationDescriptor>): List<Pair<ProtoBuf.Class, Int>> {

        return descriptors.filterIsInstance<ClassDescriptor>().flatMap {
            serializeClass(packageName, /*builder, */it)
        }
    }

    private fun emptyPackageProto(): ProtoBuf.Package = ProtoBuf.Package.newBuilder().build()

    private fun SerializerContext.buildPackageProto(
        fqName: FqName,
        descriptors: List<DeclarationDescriptor>) = topSerializer.packagePartProto(fqName, descriptors).build()
        ?: error("Package fragments not serialized: for $descriptors")

    protected fun serializeDescriptors(
        fqName: FqName,
        classifierDescriptors: List<DeclarationDescriptor>,
        topLevelDescriptors: List<DeclarationDescriptor>,
        bindingContext: BindingContext
    ): List<ProtoBuf.PackageFragment> {

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
                        topLevelDescriptors.isEmpty() && classifierDescriptors.isEmpty(),
                        bindingContext
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
                    descriptors.isEmpty(),
                    bindingContext
                )
            }
        }

        result += topLevelDescriptors.maybeChunked(TOP_LEVEL_DECLARATION_COUNT_PER_FILE) { descriptors ->
            withNewContext {
                buildFragment(
                    buildPackageProto(fqName, descriptors),
                    emptyList(),
                    fqName,
                    descriptors.isEmpty(),
                    bindingContext
                )
            }
        }

        if (result.isEmpty()) {
            result += withNewContext {
                buildFragment(
                    emptyPackageProto(),
                    emptyList(),
                    fqName,
                    true,
                    bindingContext
                )
            }
        }

        return result
    }

    protected fun getPackagesFqNames(module: ModuleDescriptor): Set<FqName> {
        val result = mutableSetOf<FqName>()

        fun getSubPackages(fqName: FqName) {
            result.add(fqName)
            module.getSubPackagesOf(fqName) { true }.forEach { getSubPackages(it) }
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
