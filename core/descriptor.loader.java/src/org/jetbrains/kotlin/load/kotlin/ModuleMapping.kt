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

import org.jetbrains.kotlin.serialization.deserialization.DeserializationConfiguration
import org.jetbrains.kotlin.serialization.jvm.JvmPackageTable
import java.io.ByteArrayInputStream
import java.io.DataInputStream

class ModuleMapping private constructor(val packageFqName2Parts: Map<String, PackageParts>, private val debugName: String) {
    fun findPackageParts(packageFqName: String): PackageParts? {
        return packageFqName2Parts[packageFqName]
    }

    override fun toString() = debugName

    companion object {
        @JvmField
        val MAPPING_FILE_EXT: String = "kotlin_module"

        @JvmField
        val EMPTY: ModuleMapping = ModuleMapping(emptyMap(), "EMPTY")

        fun create(
                bytes: ByteArray?,
                debugName: String,
                configuration: DeserializationConfiguration
        ): ModuleMapping {
            if (bytes == null) {
                return EMPTY
            }

            val stream = DataInputStream(ByteArrayInputStream(bytes))
            val version = JvmMetadataVersion(*IntArray(stream.readInt()) { stream.readInt() })

            if (configuration.skipMetadataVersionCheck || version.isCompatible()) {
                val table = JvmPackageTable.PackageTable.parseFrom(stream) ?: return EMPTY
                val result = linkedMapOf<String, PackageParts>()

                for (proto in table.packagePartsList) {
                    val packageParts = result.getOrPut(proto.packageFqName) { PackageParts(proto.packageFqName) }
                    for ((index, partShortName) in proto.classNameList.withIndex()) {
                        val multifileFacadeId = proto.multifileFacadeIdList.getOrNull(index)?.minus(1)
                        packageParts.addPart(partShortName, multifileFacadeId?.let(proto.multifileFacadeNameList::getOrNull))
                    }
                }

                for (proto in table.metadataPartsList) {
                    val packageParts = result.getOrPut(proto.packageFqName) { PackageParts(proto.packageFqName) }
                    proto.classNameList.forEach(packageParts::addMetadataPart)
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

class PackageParts(val packageFqName: String) {
    // Short name of package part -> short name of the corresponding multifile facade (or null, if it's not a multifile part)
    private val packageParts = linkedMapOf<String, String?>()

    // See JvmPackageTable.PackageTable.package_parts
    val parts: Set<String> get() = packageParts.keys
    // See JvmPackageTable.PackageTable.metadata_parts
    val metadataParts: Set<String> = linkedSetOf()

    fun addPart(partShortName: String, facadeShortName: String?) {
        packageParts[partShortName] = facadeShortName
    }

    fun removePart(shortName: String) {
        packageParts.remove(shortName)
    }

    fun addMetadataPart(shortName: String) {
        (metadataParts as MutableSet /* see KT-14663 */).add(shortName)
    }

    fun addTo(builder: JvmPackageTable.PackageTable.Builder) {
        if (parts.isNotEmpty()) {
            builder.addPackageParts(JvmPackageTable.PackageParts.newBuilder().apply {
                packageFqName = this@PackageParts.packageFqName

                val facadeNameToId = mutableMapOf<String, Int>()
                for ((facadeName, partNames) in parts.groupBy { getMultifileFacadeName(it) }.toSortedMap(nullsLast())) {
                    for (partName in partNames.sorted()) {
                        addClassName(partName)
                        if (facadeName != null) {
                            addMultifileFacadeId(1 + facadeNameToId.getOrPut(facadeName) { facadeNameToId.size })
                        }
                    }
                }

                for ((facadeId, facadeName) in facadeNameToId.values.zip(facadeNameToId.keys).sortedBy(Pair<Int, String>::first)) {
                    assert(facadeId == multifileFacadeNameCount) { "Multifile facades are loaded incorrectly: $facadeNameToId" }
                    addMultifileFacadeName(facadeName)
                }
            })
        }

        if (metadataParts.isNotEmpty()) {
            builder.addMetadataParts(JvmPackageTable.PackageParts.newBuilder().apply {
                packageFqName = this@PackageParts.packageFqName
                addAllClassName(metadataParts.sorted())
            })
        }
    }

    fun getMultifileFacadeName(partShortName: String): String? = packageParts[partShortName]

    operator fun plusAssign(other: PackageParts) {
        for ((partShortName, facadeShortName) in other.packageParts) {
            addPart(partShortName, facadeShortName)
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
