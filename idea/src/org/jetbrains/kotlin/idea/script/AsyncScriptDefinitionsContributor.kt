/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.script

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.core.script.ScriptDefinitionContributor
import org.jetbrains.kotlin.idea.core.script.ScriptDefinitionsManager
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

abstract class AsyncScriptDefinitionsContributor(protected val project: Project) : ScriptDefinitionContributor {
    abstract val progressMessage: String

    override fun isReady(): Boolean = definitions != null

    override fun getDefinitions(): List<KotlinScriptDefinition> {
        definitionsLock.read {
            if (definitions != null) {
                return definitions!!
            }
        }

        forceStartUpdate = false
        asyncRunUpdateScriptTemplates()
        return emptyList()
    }

    protected fun asyncRunUpdateScriptTemplates() {
        val backgroundTask = inProgressLock.write {
            shouldStartNewUpdate = true
            if (!inProgress) {
                inProgress = true
                return@write DefinitionsCollectorBackgroundTask()
            }
            return@write null
        }
        // TODO: resolve actual reason for the exception below
        try {
            backgroundTask?.queue()
        } catch (e: IllegalStateException) {
            if (e.message?.contains("Calling invokeAndWait from read-action leads to possible deadlock") == false) throw e
        }
    }

    protected abstract fun loadScriptDefinitions(previous: List<KotlinScriptDefinition>?): List<KotlinScriptDefinition>

    private var definitions: List<KotlinScriptDefinition>? = null
    private val definitionsLock = ReentrantReadWriteLock()

    protected var forceStartUpdate = false
    protected var shouldStartNewUpdate = false

    private var inProgress = false
    private val inProgressLock = ReentrantReadWriteLock()

    private inner class DefinitionsCollectorBackgroundTask : Task.Backgroundable(project, progressMessage, true) {

        override fun run(indicator: ProgressIndicator) {
            while (true) {
                inProgressLock.read {
                    if (indicator.isCanceled || !shouldStartNewUpdate) {
                        inProgressLock.write {
                            inProgress = false
                        }
                        return
                    }
                    shouldStartNewUpdate = false
                }

                val wasRunning = definitionsLock.isWriteLocked
                val needReload = definitionsLock.write {
                    if (wasRunning && !forceStartUpdate && definitions != null) return@write false
                    val newDefinitions = loadScriptDefinitions(definitions)
                    if (newDefinitions != definitions) {
                        definitions = newDefinitions
                        return@write true
                    }
                    return@write false
                }

                if (needReload) {
                    if (isHeadless) {
                        // If new script definitions found, then ScriptDefinitionsManager.reloadDefinitionsBy should be called
                        // This may cause deadlock because Task.Backgroundable.queue executes task synchronously in headless mode
                        ApplicationManager.getApplication().executeOnPooledThread {
                            ScriptDefinitionsManager.getInstance(project).reloadDefinitionsBy(this@AsyncScriptDefinitionsContributor)
                        }
                    } else {
                        ScriptDefinitionsManager.getInstance(project).reloadDefinitionsBy(this@AsyncScriptDefinitionsContributor)
                    }
                }
            }
        }
    }
}