/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.metadata.jvm.deserialization

import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.metadata.deserialization.NameResolverImpl
import org.jetbrains.kotlin.metadata.deserialization.isKotlin1Dot4OrLater
import org.jetbrains.kotlin.metadata.jvm.JvmModuleProtoBuf
import java.io.*

class ModuleMapping private constructor(
    val packageFqName2Parts: Map<String, PackageParts>,
    val moduleData: BinaryModuleData,
    private val debugName: String
) {
    fun findPackageParts(packageFqName: String): PackageParts? {
        return packageFqName2Parts[packageFqName]
    }

    override fun toString() = debugName

    companion object {
        const val MAPPING_FILE_EXT: String = "kotlin_module"

        @JvmField
        val EMPTY: ModuleMapping = ModuleMapping(emptyMap(), BinaryModuleData(emptyList()), "EMPTY")

        @JvmField
        val CORRUPTED: ModuleMapping = ModuleMapping(emptyMap(), BinaryModuleData(emptyList()), "CORRUPTED")

        const val STRICT_METADATA_VERSION_SEMANTICS_FLAG = 1 shl 0

        fun loadModuleMapping(
            bytes: ByteArray?,
            debugName: String,
            skipMetadataVersionCheck: Boolean,
            isJvmPackageNameSupported: Boolean,
            reportIncompatibleVersionError: (JvmMetadataVersion) -> Unit
        ): ModuleMapping {
            if (bytes == null) {
                return EMPTY
            }

            val stream = DataInputStream(ByteArrayInputStream(bytes))

            val versionNumber = try {
                IntArray(stream.readInt()) { stream.readInt() }
            } catch (e: IOException) {
                return CORRUPTED
            }

            val preVersion = JvmMetadataVersion(*versionNumber)
            if (!skipMetadataVersionCheck && !preVersion.isCompatible()) {
                reportIncompatibleVersionError(preVersion)
                return EMPTY
            }

            // Since Kotlin 1.4, we write integer flags between the version and the proto
            val flags = if (isKotlin1Dot4OrLater(preVersion)) stream.readInt() else 0

            val version = JvmMetadataVersion(versionNumber, (flags and STRICT_METADATA_VERSION_SEMANTICS_FLAG) != 0)
            if (!skipMetadataVersionCheck && !version.isCompatible()) {
                reportIncompatibleVersionError(version)
                return EMPTY
            }

            val moduleProto = JvmModuleProtoBuf.Module.parseFrom(stream) ?: return EMPTY
            val result = linkedMapOf<String, PackageParts>()

            for (proto in moduleProto.packagePartsList) {
                val packageFqName = proto.packageFqName
                val packageParts = result.getOrPut(packageFqName) { PackageParts(packageFqName) }

                for ((index, partShortName) in proto.shortClassNameList.withIndex()) {
                    packageParts.addPart(
                        internalNameOf(packageFqName, partShortName),
                        loadMultiFileFacadeInternalName(
                            proto.multifileFacadeShortNameIdList, proto.multifileFacadeShortNameList, index, packageFqName
                        )
                    )
                }

                if (isJvmPackageNameSupported) {
                    for ((index, partShortName) in proto.classWithJvmPackageNameShortNameList.withIndex()) {
                        val packageId = proto.classWithJvmPackageNamePackageIdList.getOrNull(index)
                            ?: proto.classWithJvmPackageNamePackageIdList.lastOrNull()
                            ?: continue
                        val jvmPackageName = moduleProto.jvmPackageNameList.getOrNull(packageId) ?: continue

                        packageParts.addPart(
                            internalNameOf(jvmPackageName, partShortName),
                            loadMultiFileFacadeInternalName(
                                proto.classWithJvmPackageNameMultifileFacadeShortNameIdList,
                                proto.multifileFacadeShortNameList,
                                index,
                                jvmPackageName
                            )
                        )
                    }
                }
            }

            for (proto in moduleProto.metadataPartsList) {
                val packageParts = result.getOrPut(proto.packageFqName) { PackageParts(proto.packageFqName) }
                proto.shortClassNameList.forEach(packageParts::addMetadataPart)
            }

            // TODO: read arguments of module annotations
            val nameResolver = NameResolverImpl(moduleProto.stringTable, moduleProto.qualifiedNameTable)
            val annotations = moduleProto.annotationList.map { proto -> nameResolver.getQualifiedClassName(proto.id) }

            return ModuleMapping(result, BinaryModuleData(annotations), debugName)
        }

        private fun loadMultiFileFacadeInternalName(
            multifileFacadeIds: List<Int>,
            multifileFacadeShortNames: List<String>,
            index: Int,
            packageFqName: String
        ): String? {
            val multifileFacadeId = multifileFacadeIds.getOrNull(index)?.minus(1)
            val facadeShortName = multifileFacadeId?.let(multifileFacadeShortNames::getOrNull)
            return facadeShortName?.let { internalNameOf(packageFqName, it) }
        }
    }
}

private fun internalNameOf(packageFqName: String, className: String): String =
    if (packageFqName.isEmpty()) className
    else packageFqName.replace('.', '/') + "/" + className

class PackageParts(val packageFqName: String) {
    // JVM internal name of package part -> JVM internal name of the corresponding multifile facade (or null, if it's not a multifile part)
    private val packageParts = linkedMapOf<String, String?>()
    val parts: Set<String> get() = packageParts.keys

    // Short names of .kotlin_metadata package parts
    val metadataParts: Set<String> = linkedSetOf()

    fun addPart(partInternalName: String, facadeInternalName: String?) {
        packageParts[partInternalName] = facadeInternalName
    }

    fun removePart(internalName: String) {
        packageParts.remove(internalName)
    }

    fun addMetadataPart(shortName: String) {
        (metadataParts as MutableSet /* see KT-14663 */).add(shortName)
    }

    fun addTo(builder: JvmModuleProtoBuf.Module.Builder) {
        if (parts.isNotEmpty()) {
            builder.addPackageParts(JvmModuleProtoBuf.PackageParts.newBuilder().apply {
                packageFqName = this@PackageParts.packageFqName

                val packageInternalName = packageFqName.replace('.', '/')
                val (partsWithinPackage, partsOutsidePackage) = parts.partition { partInternalName ->
                    partInternalName.packageName == packageInternalName
                }

                val facadeNameToId = mutableMapOf<String, Int>()
                writePartsWithinPackage(partsWithinPackage, facadeNameToId)
                writePartsOutsidePackage(partsOutsidePackage, facadeNameToId, builder)
                writeMultifileFacadeNames(facadeNameToId)
            })
        }

        if (metadataParts.isNotEmpty()) {
            builder.addMetadataParts(JvmModuleProtoBuf.PackageParts.newBuilder().apply {
                packageFqName = this@PackageParts.packageFqName
                addAllShortClassName(metadataParts.sorted())
            })
        }
    }

    private fun JvmModuleProtoBuf.PackageParts.Builder.writePartsWithinPackage(
        parts: List<String>,
        facadeNameToId: MutableMap<String, Int>
    ) {
        for ((facadeInternalName, partInternalNames) in parts.groupBy { getMultifileFacadeName(it) }.toSortedMap(nullsLast())) {
            for (partInternalName in partInternalNames.sorted()) {
                addShortClassName(partInternalName.className)
                if (facadeInternalName != null) {
                    addMultifileFacadeShortNameId(getMultifileFacadeShortNameId(facadeInternalName, facadeNameToId))
                }
            }
        }
    }

    // Writes information about package parts which have a different JVM package from the Kotlin package (with the help of @JvmPackageName)
    private fun JvmModuleProtoBuf.PackageParts.Builder.writePartsOutsidePackage(
        parts: List<String>,
        facadeNameToId: MutableMap<String, Int>,
        packageTableBuilder: JvmModuleProtoBuf.Module.Builder
    ) {
        val packageIds = mutableListOf<Int>()
        for ((packageInternalName, partsInPackage) in parts.groupBy { it.packageName }.toSortedMap()) {
            val packageFqName = packageInternalName.replace('/', '.')
            if (packageFqName !in packageTableBuilder.jvmPackageNameList) {
                packageTableBuilder.addJvmPackageName(packageFqName)
            }
            val packageId = packageTableBuilder.jvmPackageNameList.indexOf(packageFqName)
            for ((facadeInternalName, partInternalNames) in partsInPackage.groupBy { getMultifileFacadeName(it) }.toSortedMap(nullsLast())) {
                for (partInternalName in partInternalNames.sorted()) {
                    addClassWithJvmPackageNameShortName(partInternalName.className)
                    if (facadeInternalName != null) {
                        addClassWithJvmPackageNameMultifileFacadeShortNameId(
                            getMultifileFacadeShortNameId(facadeInternalName, facadeNameToId)
                        )
                    }
                    packageIds.add(packageId)
                }
            }
        }

        // See PackageParts#class_with_jvm_package_name_package_id in jvm_module.proto for description of this optimization
        while (packageIds.size > 1 && packageIds[packageIds.size - 1] == packageIds[packageIds.size - 2]) {
            packageIds.removeAt(packageIds.size - 1)
        }

        addAllClassWithJvmPackageNamePackageId(packageIds)
    }

    private fun getMultifileFacadeShortNameId(facadeInternalName: String, facadeNameToId: MutableMap<String, Int>): Int {
        return 1 + facadeNameToId.getOrPut(facadeInternalName.className) { facadeNameToId.size }
    }

    private fun JvmModuleProtoBuf.PackageParts.Builder.writeMultifileFacadeNames(facadeNameToId: Map<String, Int>) {
        for ((facadeId, facadeName) in facadeNameToId.values.zip(facadeNameToId.keys).sortedBy(Pair<Int, String>::first)) {
            assert(facadeId == multifileFacadeShortNameCount) { "Multifile facades are loaded incorrectly: $facadeNameToId" }
            addMultifileFacadeShortName(facadeName)
        }
    }

    private val String.packageName: String get() = substringBeforeLast('/', "")
    private val String.className: String get() = substringAfterLast('/')

    fun getMultifileFacadeName(partInternalName: String): String? = packageParts[partInternalName]

    operator fun plusAssign(other: PackageParts) {
        for ((partInternalName, facadeInternalName) in other.packageParts) {
            addPart(partInternalName, facadeInternalName)
        }
        other.metadataParts.forEach(this::addMetadataPart)
    }

    override fun equals(other: Any?) =
        other is PackageParts &&
                other.packageFqName == packageFqName && other.packageParts == packageParts && other.metadataParts == metadataParts

    override fun hashCode() =
        (packageFqName.hashCode() * 31 + packageParts.hashCode()) * 31 + metadataParts.hashCode()

    override fun toString() =
        (parts + metadataParts).toString()
}

fun JvmModuleProtoBuf.Module.serializeToByteArray(version: BinaryVersion, flags: Int): ByteArray {
    val moduleMapping = ByteArrayOutputStream(4096)
    val out = DataOutputStream(moduleMapping)
    val versionArray = version.toArray()
    out.writeInt(versionArray.size)
    for (number in versionArray) {
        out.writeInt(number)
    }
    if (isKotlin1Dot4OrLater(version)) {
        out.writeInt(flags)
    }
    writeTo(out)
    out.flush()
    return moduleMapping.toByteArray()
}
