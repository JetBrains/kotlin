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
import org.jetbrains.kotlin.load.kotlin.JvmPackagePartProviderBase
import org.jetbrains.kotlin.load.kotlin.loadModuleMapping
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion
import org.jetbrains.kotlin.metadata.jvm.deserialization.ModuleMapping
import org.jetbrains.kotlin.resolve.CompilerDeserializationConfiguration
import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.PrintStream

class JvmPackagePartProvider(
    languageVersionSettings: LanguageVersionSettings,
    private val scope: GlobalSearchScope
) : JvmPackagePartProviderBase<VirtualFile>() {
    override val deserializationConfiguration = CompilerDeserializationConfiguration(languageVersionSettings)

    override val loadedModules: MutableList<ModuleMappingInfo<VirtualFile>> = SmartList()

    fun addRoots(roots: List<JavaRoot>, messageCollector: MessageCollector) {
        for ((root, type) in roots) {
            if (type != JavaRoot.RootType.BINARY) continue
            if (root !in scope) continue

            val metaInf = root.findChild("META-INF") ?: continue
            for (moduleFile in metaInf.children) {
                if (!moduleFile.name.endsWith(ModuleMapping.MAPPING_FILE_EXT)) continue

                tryLoadModuleMapping(
                    { moduleFile.contentsToByteArray() }, moduleFile.toString(), moduleFile.path,
                    deserializationConfiguration, messageCollector
                )?.let {
                    loadedModules.add(ModuleMappingInfo(root, it, moduleFile.nameWithoutExtension))
                }
            }
        }
    }
}

fun tryLoadModuleMapping(
    getModuleBytes: () -> ByteArray,
    debugName: String,
    modulePath: String,
    deserializationConfiguration: CompilerDeserializationConfiguration,
    messageCollector: MessageCollector
): ModuleMapping? = try {
    ModuleMapping.loadModuleMapping(getModuleBytes(), debugName, deserializationConfiguration) { incompatibleVersion ->
        messageCollector.report(
            ERROR,
            "Module was compiled with an incompatible version of Kotlin. The binary version of its metadata is " +
                    "$incompatibleVersion, expected version is ${JvmMetadataVersion.INSTANCE}.",
            CompilerMessageLocation.create(modulePath)
        )
    }
} catch (e: EOFException) {
    messageCollector.report(
        ERROR, "Error occurred when reading the module: ${e.message}", CompilerMessageLocation.create(modulePath)
    )
    messageCollector.report(
        LOGGING,
        String(ByteArrayOutputStream().also { e.printStackTrace(PrintStream(it)) }.toByteArray()),
        CompilerMessageLocation.create(modulePath)
    )
    null
}
