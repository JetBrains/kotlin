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

package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.SmartList
import org.jetbrains.kotlin.cli.jvm.index.JavaRoot
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.PackagePartProvider
import org.jetbrains.kotlin.load.kotlin.ModuleMapping
import org.jetbrains.kotlin.load.kotlin.PackageParts
import org.jetbrains.kotlin.resolve.CompilerDeserializationConfiguration
import java.io.EOFException

class JvmPackagePartProvider(
        languageVersionSettings: LanguageVersionSettings,
        private val scope: GlobalSearchScope
) : PackagePartProvider {
    private data class ModuleMappingInfo(val root: VirtualFile, val mapping: ModuleMapping)

    private val deserializationConfiguration = CompilerDeserializationConfiguration(languageVersionSettings)

    private val loadedModules: MutableList<ModuleMappingInfo> = SmartList()

    override fun findPackageParts(packageFqName: String): List<String> {
        val rootToPackageParts = getPackageParts(packageFqName)
        if (rootToPackageParts.isEmpty()) return emptyList()

        val result = linkedSetOf<String>()
        val visitedMultifileFacades = linkedSetOf<String>()
        for ((_, packageParts) in rootToPackageParts) {
            for (name in packageParts.parts) {
                val facadeName = packageParts.getMultifileFacadeName(name)
                if (facadeName == null || facadeName !in visitedMultifileFacades) {
                    result.add(name)
                }
            }
            packageParts.parts.mapNotNullTo(visitedMultifileFacades, packageParts::getMultifileFacadeName)
        }
        return result.toList()
    }

    override fun findMetadataPackageParts(packageFqName: String): List<String> =
            getPackageParts(packageFqName).values.flatMap(PackageParts::metadataParts).distinct()

    @Synchronized
    private fun getPackageParts(packageFqName: String): Map<VirtualFile, PackageParts> {
        val result = mutableMapOf<VirtualFile, PackageParts>()
        for ((root, mapping) in loadedModules) {
            val newParts = mapping.findPackageParts(packageFqName) ?: continue
            result[root]?.let { parts -> parts += newParts } ?: result.put(root, newParts)
        }
        return result
    }

    fun addRoots(roots: List<JavaRoot>) {
        for ((root, type) in roots) {
            if (type != JavaRoot.RootType.BINARY) continue
            if (root !in scope) continue

            val metaInf = root.findChild("META-INF") ?: continue
            for (moduleFile in metaInf.children) {
                if (!moduleFile.name.endsWith(ModuleMapping.MAPPING_FILE_EXT)) continue

                val mapping = try {
                    ModuleMapping.create(moduleFile.contentsToByteArray(), moduleFile.toString(), deserializationConfiguration)
                }
                catch (e: EOFException) {
                    throw RuntimeException("Error on reading package parts from $moduleFile in $root", e)
                }
                loadedModules.add(ModuleMappingInfo(root, mapping))
            }
        }
    }
}
