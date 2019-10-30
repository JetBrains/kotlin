/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration.cache

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.core.script.configuration.loader.ScriptConfigurationLoader
import org.jetbrains.kotlin.idea.core.script.debug
import org.jetbrains.kotlin.idea.core.util.*
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.KtFileScriptSource
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationResult
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper.FromCompilationConfiguration
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper.FromLegacy
import java.io.*
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.asSuccess
import kotlin.script.experimental.dependencies.ScriptDependencies

class ScriptConfigurationFileAttributeCache :
    ScriptConfigurationLoader {
    operator fun contains(file: VirtualFile): Boolean =
        file.scriptDependencies != null || file.scriptCompilationConfiguration != null

    override val skipSaveToAttributes: Boolean
        get() = true

    override val skipNotification: Boolean
        get() = true

    override fun loadDependencies(
        firstLoad: Boolean,
        file: KtFile,
        scriptDefinition: ScriptDefinition
    ): ScriptCompilationConfigurationResult? {
        if (!firstLoad) return null

        val virtualFile = file.originalFile.virtualFile

        val configurationFromAttributes =
            virtualFile.scriptCompilationConfiguration?.let {
                FromCompilationConfiguration(KtFileScriptSource(file), it)
            } ?: virtualFile.scriptDependencies?.let {
                FromLegacy(KtFileScriptSource(file), it, scriptDefinition)
            } ?: return null


        debug(virtualFile) { "configuration from fileAttributes = $configurationFromAttributes" }

        if (!areDependenciesValid(virtualFile, configurationFromAttributes)) {
            save(virtualFile, null)
            return null
        }

        return configurationFromAttributes.asSuccess()
    }

    private fun areDependenciesValid(file: VirtualFile, configuration: ScriptCompilationConfigurationWrapper): Boolean {
        return configuration.dependenciesClassPath.all {
            if (it.exists()) {
                true
            } else {
                debug(file) {
                    "classpath root saved to file attribute doesn't exist: ${it.path}"
                }
                false
            }

        }
    }

    fun save(file: VirtualFile, value: ScriptCompilationConfigurationWrapper?) {
        if (value == null) {
            file.scriptDependencies = null
            file.scriptCompilationConfiguration = null
        } else {
            if (value is FromLegacy) {
                file.scriptDependencies = value.legacyDependencies
            } else {
                if (file.scriptDependencies != null) file.scriptDependencies = null
                file.scriptCompilationConfiguration = value.configuration
            }
        }
    }

}

private var VirtualFile.scriptDependencies: ScriptDependencies? by cachedFileAttribute(
    name = "kotlin-script-dependencies",
    version = 3,
    read = {
        ScriptDependencies(
            classpath = readFileList(),
            imports = readStringList(),
            javaHome = readNullable(DataInput::readFile),
            scripts = readFileList(),
            sources = readFileList()
        )
    },
    write = {
        with(it) {
            writeFileList(classpath)
            writeStringList(imports)
            writeNullable(javaHome, DataOutput::writeFile)
            writeFileList(scripts)
            writeFileList(sources)
        }
    }
)

private var VirtualFile.scriptCompilationConfiguration: ScriptCompilationConfiguration? by cachedFileAttribute(
    name = "kotlin-script-compilation-configuration",
    version = 1,
    read = {
        val size = readInt()
        val bytes = ByteArray(size)
        read(bytes, 0, size)
        val bis = ByteArrayInputStream(bytes)
        ObjectInputStream(bis).use { ois ->
            ois.readObject() as ScriptCompilationConfiguration
        }
    },
    write = {
        val os = ByteArrayOutputStream()
        ObjectOutputStream(os).use { oos ->
            oos.writeObject(it)
        }
        val bytes = os.toByteArray()
        writeInt(bytes.size)
        write(bytes)
    }
)
