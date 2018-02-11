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

package org.jetbrains.kotlin.load.kotlin

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.serialization.deserialization.DeserializationConfiguration
import org.jetbrains.kotlin.serialization.jvm.JvmModuleProtoBuf
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.IOException

class ModuleMapping private constructor(
        val packageFqName2Parts: Map<String, PackageParts>,
        private val debugName: String
) {
    fun findPackageParts(packageFqName: String): PackageParts? {
        return packageFqName2Parts[packageFqName]
    }

    override fun toString() = debugName

    companion object {
        @JvmField
        val MAPPING_FILE_EXT: String = "kotlin_module"

        @JvmField
        val EMPTY: ModuleMapping = ModuleMapping(emptyMap(), "EMPTY")

        @JvmField
        val CORRUPTED: ModuleMapping = ModuleMapping(emptyMap(), "CORRUPTED")

        fun create(
                bytes: ByteArray?,
                debugName: String,
                configuration: DeserializationConfiguration
        ): ModuleMapping {
            if (bytes == null) {
                return EMPTY
            }

            val stream = DataInputStream(ByteArrayInputStream(bytes))

            val versionNumber = try {
                IntArray(stream.readInt()) { stream.readInt() }
            }
            catch (e: IOException) {
                return CORRUPTED
            }

            val version = JvmMetadataVersion(*versionNumber)

            if (configuration.skipMetadataVersionCheck || version.isCompatible()) {
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

                    if (configuration.isJvmPackageNameSupported) {
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

                return ModuleMapping(result, debugName)
            }
            else {
                // TODO: consider reporting "incompatible ABI version" error for package parts
            }

            return EMPTY
        }
    }
}

private fun internalNameOf(packageFqName: String, className: String): String =
        JvmClassName.byFqNameWithoutInnerClasses(FqName(packageFqName).child(Name.identifier(className))).internalName

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
