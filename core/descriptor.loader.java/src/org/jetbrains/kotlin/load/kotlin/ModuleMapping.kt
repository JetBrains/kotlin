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

        fun create(bytes: ByteArray?, debugName: String?): ModuleMapping {
            if (bytes == null) {
                return EMPTY
            }

            val stream = DataInputStream(ByteArrayInputStream(bytes))
            val version = JvmMetadataVersion(*IntArray(stream.readInt()) { stream.readInt() })

            if (version.isCompatible()) {
                val parseFrom = JvmPackageTable.PackageTable.parseFrom(stream)
                if (parseFrom != null) {
                    val packageFqNameParts = hashMapOf<String, PackageParts>().apply {
                        addParts(this, parseFrom.packagePartsList, PackageParts::addPart)
                        addParts(this, parseFrom.metadataPartsList, PackageParts::addMetadataPart)
                    }
                    return ModuleMapping(packageFqNameParts, debugName ?: "<unknown>")
                }
            }
            else {
                // TODO: consider reporting "incompatible ABI version" error for package parts
            }

            return EMPTY
        }

        private inline fun addParts(
                result: MutableMap<String, PackageParts>,
                partsList: List<JvmPackageTable.PackageParts>,
                addPartOrMetadataPart: (PackageParts, String) -> Unit
        ) {
            for (proto in partsList) {
                PackageParts(proto.packageFqName).apply {
                    result.put(proto.packageFqName, this)
                    proto.classNameList.forEach { addPartOrMetadataPart(this, it) }
                }
            }
        }
    }
}

class PackageParts(val packageFqName: String) {
    // See JvmPackageTable.PackageTable.package_parts
    val parts: Set<String> = linkedSetOf<String>()
    // See JvmPackageTable.PackageTable.metadata_parts
    val metadataParts: Set<String> = linkedSetOf<String>()

    fun addPart(shortName: String) {
        (parts as MutableSet /* see KT-14663 */).add(shortName)
    }

    fun removePart(shortName: String) {
        (parts as MutableSet).remove(shortName)
    }

    fun addMetadataPart(shortName: String) {
        (metadataParts as MutableSet).add(shortName)
    }

    fun addTo(builder: JvmPackageTable.PackageTable.Builder) {
        if (parts.isNotEmpty()) {
            builder.addPackageParts(JvmPackageTable.PackageParts.newBuilder().apply {
                packageFqName = this@PackageParts.packageFqName
                addAllClassName(parts.sorted())
            })
        }
        if (metadataParts.isNotEmpty()) {
            builder.addMetadataParts(JvmPackageTable.PackageParts.newBuilder().apply {
                packageFqName = this@PackageParts.packageFqName
                addAllClassName(metadataParts.sorted())
            })
        }
    }

    operator fun plusAssign(other: PackageParts) {
        other.parts.forEach(this::addPart)
        other.metadataParts.forEach(this::addMetadataPart)
    }

    override fun equals(other: Any?) =
            other is PackageParts && other.packageFqName == packageFqName && other.parts == parts && other.metadataParts == metadataParts

    override fun hashCode() =
            (packageFqName.hashCode() * 31 + parts.hashCode()) * 31 + metadataParts.hashCode()

    override fun toString() =
            (parts + metadataParts).toString()
}
