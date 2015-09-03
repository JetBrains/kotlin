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
import java.io.Writer

public class ModuleMapping(proto: ByteArray? = null) {

    val packageFqName2Parts = hashMapOf<String, PackageParts>()

    init {
        if (proto != null) {
            val parseFrom: JvmPackageTable.PackageTable? = JvmPackageTable.PackageTable.parseFrom(proto)
            if (parseFrom != null) {
                parseFrom.packagePartsList.map {
                    val packageParts = PackageParts(it.packageFqName)
                    packageFqName2Parts.put(it.packageFqName, packageParts)
                    it.classNameList.map {
                        packageParts.parts.add(it)
                    }
                }
            }
        }
    }

    fun findPackageParts(packageFqName: String): PackageParts? {
        return packageFqName2Parts[packageFqName]
    }

    companion object {
        public val MAPPING_FILE_EXT: String = "kotlin_module";
    }
}

public class PackageParts(val packageFqName: String) {

    val parts = linkedSetOf<String>()

    override fun equals(other: Any?): Boolean {
        if (other !is PackageParts) {
            return false;
        }

        if (other.packageFqName != packageFqName) {
            return false;
        }

        if (other.parts.size() != parts.size()) {
            return false;
        }

        for (part in other.parts) {
            if (!parts.contains(part)) {
                return false;
            }
        }

        return true;
    }

    override fun hashCode(): Int {
        return packageFqName.hashCode() / 3 + parts.size() / 3 + (parts.firstOrNull()?.hashCode() ?: 0) / 3
    }

    companion object {
        @jvmStatic public fun PackageParts.serialize(builder : JvmPackageTable.PackageTable.Builder) {
            if (this.parts.isNotEmpty()) {
                val packageParts = JvmPackageTable.PackageParts.newBuilder()
                packageParts.setPackageFqName(this.packageFqName)
                packageParts.addAllClassName(this.parts.sorted())
                builder.addPackageParts(packageParts)
            }
        }
    }
}

