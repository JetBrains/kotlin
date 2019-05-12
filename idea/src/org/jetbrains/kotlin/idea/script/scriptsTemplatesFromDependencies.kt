/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.script

import com.intellij.ProjectTopics
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.roots.OrderEnumerator
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.core.script.loadDefinitionsFromTemplates
import org.jetbrains.kotlin.idea.util.projectStructure.allModules
import org.jetbrains.kotlin.scripting.definitions.*
import java.io.File
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration

class ScriptTemplatesFromDependenciesProvider(project: Project) : AsyncScriptDefinitionsContributor(project) {

    private var templates: TemplatesWithCp? = null
    private val templatesLock = ReentrantReadWriteLock()

    init {
        val connection = project.messageBus.connect()
        connection.subscribe(ProjectTopics.PROJECT_ROOTS, object : ModuleRootListener {
            override fun rootsChanged(event: ModuleRootEvent) {
                if (project.isInitialized) {
                    forceStartUpdate = true
                    asyncRunUpdateScriptTemplates()
                }
            }
        })
    }

    override val id = "ScriptTemplatesFromDependenciesProvider"
    override val progressMessage = "Kotlin: scanning dependencies for script definitions..."

    override fun loadScriptDefinitions(previous: List<ScriptDefinition>?): List<ScriptDefinition> {
        val templatesCopy = templatesLock.write {
            val newTemplates = scriptDefinitionsFromDependencies(project)
            if (newTemplates != templates) {
                templates = newTemplates
                return@write newTemplates
            }
            return@write null
        }
        if (templatesCopy != null) {
            val hostConfiguration = ScriptingHostConfiguration(defaultJvmScriptingHostConfiguration) {
                getEnvironment {
                    mapOf(
                        "projectRoot" to (project.basePath ?: project.baseDir.canonicalPath)?.let(::File)
                    )
                }
            }
            return loadDefinitionsFromTemplates(
                templateClassNames = templatesCopy.templates,
                templateClasspath = templatesCopy.classpath,
                baseHostConfiguration = hostConfiguration
            )
        }
        return previous ?: emptyList()
    }
}

private data class TemplatesWithCp(
    val templates: List<String>,
    val classpath: List<File>
)

private fun scriptDefinitionsFromDependencies(project: Project): TemplatesWithCp {
    val templates = LinkedHashSet<String>()
    val classpath = LinkedHashSet<File>()

    fun addTemplatesFromRoot(vfile: VirtualFile): Boolean {
        var templatesFound = false
        val root = JarFileSystem.getInstance().getJarRootForLocalFile(vfile) ?: vfile
        if (root.isValid) {
            root.findFileByRelativePath(SCRIPT_DEFINITION_MARKERS_PATH)?.takeIf { it.isDirectory }?.children?.forEach {
                if (it.isValid && !it.isDirectory) {
                    templates.add(it.name.removeSuffix(SCRIPT_DEFINITION_MARKERS_EXTENSION_WITH_DOT))
                    templatesFound = true
                }
            }
        }
        return templatesFound
    }

    // processing source roots from the same project first since the resources are copied to the classes roots only on compilation
    project.allModules().forEach { module ->
        OrderEnumerator.orderEntries(module).withoutDepModules().withoutLibraries().withoutSdk().sourceRoots.forEach { root ->
            if (addTemplatesFromRoot(root)) {
                classpath.addAll(OrderEnumerator.orderEntries(module).withoutSdk().classesRoots.mapNotNull {
                    it.canonicalPath?.removeSuffix("!/").let(::File)
                })
            }
        }
    }

    project.allModules().forEach { module ->
        // assuming that all libraries are placed into classes roots
        // TODO: extract exact library dependencies instead of putting all module dependencies into classpath
        OrderEnumerator.orderEntries(module).withoutDepModules().withoutSdk().classesRoots.forEach { root ->
            if (addTemplatesFromRoot(root)) {
                // minimizing the classpath needed to use the template by taking cp only from modules with new templates found
                // on the other hand the approach may fail if some module contains a template without proper classpath, while
                // the other has properly configured classpath, so assuming that the dependencies are set correctly everywhere
                classpath.addAll(OrderEnumerator.orderEntries(module).withoutSdk().classesRoots.mapNotNull {
                    it.canonicalPath?.removeSuffix("!/").let(::File)
                })
            }
        }
    }
    return TemplatesWithCp(templates.toList(), classpath.toList())
}

