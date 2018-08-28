/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.script

import com.intellij.ProjectTopics
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.roots.OrderEnumerator
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.core.script.ScriptDefinitionContributor
import org.jetbrains.kotlin.idea.core.script.ScriptDefinitionsManager
import org.jetbrains.kotlin.idea.core.script.loadDefinitionsFromTemplates
import org.jetbrains.kotlin.idea.util.projectStructure.allModules
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import java.io.File
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class ScriptTemplatesFromDependenciesProvider(private val project: Project) : ScriptDefinitionContributor {

    data class TemplatesWithCp(
        val templates: List<String>,
        val classpath: List<File>
    )

    private var templates: TemplatesWithCp? = null
    private val lock = ReentrantReadWriteLock()

    init {
        val connection = project.messageBus.connect()
        connection.subscribe(ProjectTopics.PROJECT_ROOTS, object : ModuleRootListener {
            override fun rootsChanged(event: ModuleRootEvent?) {
                if (project.isInitialized) {
                    var templatesChanged = false
                    lock.read {
                        val newTemplates = getScriptTemplates()
                        if (newTemplates != templates) lock.write {
                            templates = newTemplates
                            templatesChanged = true
                        }
                    }
                    if (templatesChanged) {
                        ScriptDefinitionsManager.getInstance(project).reloadDefinitionsBy(this@ScriptTemplatesFromDependenciesProvider)
                    }
                }
            }
        })
    }

    private fun getScriptTemplates(): TemplatesWithCp {
        val templates = LinkedHashSet<String>()
        val classpath = LinkedHashSet<File>()
        val templatesPath = "META-INF/kotlin/script/templates/"
        val processedRoots = hashSetOf<VirtualFile>()

        fun addTemplatesFromRoot(vfile: VirtualFile): Boolean {
            var newTemplatesFound = false
            if (!processedRoots.contains(vfile)) {
                processedRoots.add(vfile)
                val root = JarFileSystem.getInstance().getJarRootForLocalFile(vfile) ?: vfile
                if (root.isValid) {
                    root.findFileByRelativePath(templatesPath)?.takeIf { it.isDirectory }?.children?.forEach {
                        if (it.isValid && !it.isDirectory && templates.add(it.name)) {
                            newTemplatesFound = true
                        }
                    }
                }
            }
            return newTemplatesFound
        }

        // processing source roots from the same project first since the resources are copied to the classes roots only on compilation
        project.allModules().forEach { module ->
            OrderEnumerator.orderEntries(module).withoutDepModules().withoutLibraries().withoutSdk().sourceRoots.forEach {
                if (addTemplatesFromRoot(it)) {
                    classpath.addAll(OrderEnumerator.orderEntries(module).withoutSdk().classesRoots.mapNotNull {
                        it.canonicalPath?.let { File(it.removeSuffix("!/")) }
                    })
                }
            }
        }

        project.allModules().forEach { module ->
            // assuming that all libraries are placed into classes roots
            // TODO: extract exact library dependencies instead of putting all module dependencies into classpath
            OrderEnumerator.orderEntries(module).withoutDepModules().withoutSdk().classesRoots.forEach {
                if (addTemplatesFromRoot(it)) {
                    // minimizing the classpath needed to use the template by taking cp only from modules with new templates found
                    // on the other hand the approach may fail if some module contains a template without proper classpath, while
                    // the other has properly configured classpath, so assuming that the dependencies are set correctly everywhere
                    classpath.addAll(OrderEnumerator.orderEntries(module).withoutSdk().classesRoots.mapNotNull {
                        it.canonicalPath?.let { File(it.removeSuffix("!/")) }
                    })
                }
            }
        }
        return TemplatesWithCp(templates.toList(), classpath.toList())
    }

    override val id = "ScriptTemplatesFromDependenciesProvider"

    override fun getDefinitions(): List<KotlinScriptDefinition> {
        lock.read {
            if (templates == null) {
                val newTemplates = getScriptTemplates()
                lock.write {
                    templates = newTemplates
                }
            }
        }
        return loadDefinitionsFromTemplates(
            templateClassNames = templates!!.templates,
            templateClasspath = templates!!.classpath,
            environment = mapOf(
                "projectRoot" to (project.basePath ?: project.baseDir.canonicalPath)?.let(::File))
        )
    }
}

