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

import java.io.Writer

public class ModuleMapping(val moduleMapping: String) {

    val package2MiniFacades = hashMapOf<String, PackageFacades>()

    init {
        for (i in moduleMapping.split("\n")) {
            if(i.isEmpty()) continue
            val (pakage, facade) = i.split("->")
            val miniFacades = package2MiniFacades.getOrPut(pakage, { PackageFacades(pakage) })
            miniFacades.parts.add(facade)
        }
    }

    fun findPackageParts(internalPackageName: String): PackageFacades? {
        return package2MiniFacades[internalPackageName]
    }

    companion object {
        public val MAPPING_FILE_EXT: String = "kotlin_module";
    }
}

public class PackageFacades(val packageInternalName: String) {

    val parts = linkedSetOf<String>()

    fun serialize(out: Writer) {
        for (i in parts) {
            out.write("$packageInternalName->$i\n")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is PackageFacades) {
            return false;
        }

        if (other.packageInternalName != packageInternalName) {
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
        return packageInternalName.hashCode() / 3 + parts.size() / 3 + (parts.firstOrNull()?.hashCode() ?: 0) / 3
    }
}