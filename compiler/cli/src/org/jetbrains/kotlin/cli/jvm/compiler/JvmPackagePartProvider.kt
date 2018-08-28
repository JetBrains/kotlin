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
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.LOGGING
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.index.JavaRoot
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.load.kotlin.loadModuleMapping
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion
import org.jetbrains.kotlin.metadata.jvm.deserialization.ModuleMapping
import org.jetbrains.kotlin.metadata.jvm.deserialization.PackageParts
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.CompilerDeserializationConfiguration
import org.jetbrains.kotlin.serialization.deserialization.MetadataPartProvider
import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.PrintStream

class JvmPackagePartProvider(
    languageVersionSettings: LanguageVersionSettings,
    private val scope: GlobalSearchScope
) : PackagePartProvider, MetadataPartProvider {
    private data class ModuleMappingInfo(val root: VirtualFile, val mapping: ModuleMapping, val name: String)

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

    override fun getAnnotationsOnBinaryModule(moduleName: String): List<ClassId> {
        return loadedModules.mapNotNull { (_, mapping, name) ->
            if (name == moduleName) mapping.moduleData.annotations.map(ClassId::fromString) else null
        }.flatten()
    }

    fun addRoots(roots: List<JavaRoot>, messageCollector: MessageCollector) {
        for ((root, type) in roots) {
            if (type != JavaRoot.RootType.BINARY) continue
            if (root !in scope) continue

            val metaInf = root.findChild("META-INF") ?: continue
            for (moduleFile in metaInf.children) {
                if (!moduleFile.name.endsWith(ModuleMapping.MAPPING_FILE_EXT)) continue

                try {
                    val mapping = ModuleMapping.loadModuleMapping(
                        moduleFile.contentsToByteArray(), moduleFile.toString(), deserializationConfiguration
                    ) { incompatibleVersion ->
                        messageCollector.report(
                            ERROR,
                            "Module was compiled with an incompatible version of Kotlin. The binary version of its metadata is " +
                                    "$incompatibleVersion, expected version is ${JvmMetadataVersion.INSTANCE}.",
                            CompilerMessageLocation.create(moduleFile.path)
                        )
                    }
                    loadedModules.add(ModuleMappingInfo(root, mapping, moduleFile.nameWithoutExtension))
                } catch (e: EOFException) {
                    messageCollector.report(
                        ERROR, "Error occurred when reading the module: ${e.message}", CompilerMessageLocation.create(moduleFile.path)
                    )
                    messageCollector.report(
                        LOGGING,
                        String(ByteArrayOutputStream().also { e.printStackTrace(PrintStream(it)) }.toByteArray()),
                        CompilerMessageLocation.create(moduleFile.path)
                    )
                }
            }
        }
    }
}
