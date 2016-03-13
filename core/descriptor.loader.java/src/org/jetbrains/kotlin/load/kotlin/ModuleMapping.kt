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

class ModuleMapping private constructor(val packageFqName2Parts: Map<String, PackageParts>) {

    fun findPackageParts(packageFqName: String): PackageParts? {
        return packageFqName2Parts[packageFqName]
    }

    companion object {
        @JvmField
        val MAPPING_FILE_EXT: String = "kotlin_module"

        @JvmField
        val EMPTY: ModuleMapping = ModuleMapping(emptyMap())

        fun create(proto: ByteArray? = null): ModuleMapping {
            if (proto == null) {
                return EMPTY
            }

            val stream = DataInputStream(ByteArrayInputStream(proto))
            val version = JvmMetadataVersion(*IntArray(stream.readInt()) { stream.readInt() })

            if (version.isCompatible()) {
                val parseFrom = JvmPackageTable.PackageTable.parseFrom(stream)
                if (parseFrom != null) {
                    val packageFqNameParts = hashMapOf<String, PackageParts>()
                    parseFrom.packagePartsList.forEach {
                        val packageParts = PackageParts(it.packageFqName)
                        packageFqNameParts.put(it.packageFqName, packageParts)
                        it.classNameList.forEach {
                            packageParts.parts.add(it)
                        }
                    }
                    return ModuleMapping(packageFqNameParts)
                }
            }
            else {
                // TODO: consider reporting "incompatible ABI version" error for package parts
            }

            return EMPTY
        }
    }
}

class PackageParts(val packageFqName: String) {
    val parts = linkedSetOf<String>()

    override fun equals(other: Any?) =
            other is PackageParts && other.packageFqName == packageFqName && other.parts == parts

    override fun hashCode() =
            packageFqName.hashCode() * 31 + parts.hashCode()

    override fun toString() =
            parts.toString()

    companion object {
        @JvmStatic
        fun PackageParts.serialize(builder: JvmPackageTable.PackageTable.Builder) {
            if (this.parts.isNotEmpty()) {
                val packageParts = JvmPackageTable.PackageParts.newBuilder()
                packageParts.packageFqName = this.packageFqName
                packageParts.addAllClassName(this.parts.sorted())
                builder.addPackageParts(packageParts)
            }
        }
    }
}
