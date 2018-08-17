/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.metadata.jvm.deserialization

import org.jetbrains.kotlin.metadata.deserialization.NameResolverImpl
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

            val version = JvmMetadataVersion(*versionNumber)
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
                    val multifileFacadeId = proto.multifileFacadeShortNameIdList.getOrNull(index)?.minus(1)
                    val facadeShortName = multifileFacadeId?.let(proto.multifileFacadeShortNameList::getOrNull)
                    val facadeInternalName = facadeShortName?.let { internalNameOf(packageFqName, it) }
                    packageParts.addPart(internalNameOf(packageFqName, partShortName), facadeInternalName)
                }

                if (isJvmPackageNameSupported) {
                    for ((index, partShortName) in proto.classWithJvmPackageNameShortNameList.withIndex()) {
                        val packageId = proto.classWithJvmPackageNamePackageIdList.getOrNull(index)
                            ?: proto.classWithJvmPackageNamePackageIdList.lastOrNull()
                            ?: continue
                        val jvmPackageName = moduleProto.jvmPackageNameList.getOrNull(packageId) ?: continue
                        packageParts.addPart(internalNameOf(jvmPackageName, partShortName), null)
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

                writePartsWithinPackage(partsWithinPackage)

                writePartsOutsidePackage(partsOutsidePackage, builder)
            })
        }

        if (metadataParts.isNotEmpty()) {
            builder.addMetadataParts(JvmModuleProtoBuf.PackageParts.newBuilder().apply {
                packageFqName = this@PackageParts.packageFqName
                addAllShortClassName(metadataParts.sorted())
            })
        }
    }

    private fun JvmModuleProtoBuf.PackageParts.Builder.writePartsWithinPackage(parts: List<String>) {
        val facadeNameToId = mutableMapOf<String, Int>()
        for ((facadeInternalName, partInternalNames) in parts.groupBy { getMultifileFacadeName(it) }.toSortedMap(nullsLast())) {
            for (partInternalName in partInternalNames.sorted()) {
                addShortClassName(partInternalName.className)
                if (facadeInternalName != null) {
                    addMultifileFacadeShortNameId(1 + facadeNameToId.getOrPut(facadeInternalName.className) { facadeNameToId.size })
                }
            }
        }

        for ((facadeId, facadeName) in facadeNameToId.values.zip(facadeNameToId.keys).sortedBy(Pair<Int, String>::first)) {
            assert(facadeId == multifileFacadeShortNameCount) { "Multifile facades are loaded incorrectly: $facadeNameToId" }
            addMultifileFacadeShortName(facadeName)
        }
    }

    // Writes information about package parts which have a different JVM package from the Kotlin package (with the help of @JvmPackageName)
    private fun JvmModuleProtoBuf.PackageParts.Builder.writePartsOutsidePackage(
        parts: List<String>,
        packageTableBuilder: JvmModuleProtoBuf.Module.Builder
    ) {
        val packageIds = mutableListOf<Int>()
        for ((packageInternalName, partsInPackage) in parts.groupBy { it.packageName }.toSortedMap()) {
            val packageFqName = packageInternalName.replace('/', '.')
            if (packageFqName !in packageTableBuilder.jvmPackageNameList) {
                packageTableBuilder.addJvmPackageName(packageFqName)
            }
            val packageId = packageTableBuilder.jvmPackageNameList.indexOf(packageFqName)
            for (part in partsInPackage.map { it.className }.sorted()) {
                addClassWithJvmPackageNameShortName(part)
                packageIds.add(packageId)
            }
        }

        // See PackageParts#class_with_jvm_package_name_package_id in jvm_module.proto for description of this optimization
        while (packageIds.size > 1 && packageIds[packageIds.size - 1] == packageIds[packageIds.size - 2]) {
            packageIds.removeAt(packageIds.size - 1)
        }

        addAllClassWithJvmPackageNamePackageId(packageIds)
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

fun JvmModuleProtoBuf.Module.serializeToByteArray(versionArray: IntArray): ByteArray {
    val moduleMapping = ByteArrayOutputStream(4096)
    val out = DataOutputStream(moduleMapping)
    out.writeInt(versionArray.size)
    for (number in versionArray) {
        out.writeInt(number)
    }
    writeTo(out)
    out.flush()
    return moduleMapping.toByteArray()
}
