/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.script

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.core.script.ScriptDefinitionSourceAsContributor
import org.jetbrains.kotlin.idea.core.script.ScriptDefinitionsManager
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

abstract class AsyncScriptDefinitionsContributor(protected val project: Project) : ScriptDefinitionSourceAsContributor {
    abstract val progressMessage: String

    override fun isReady(): Boolean = _definitions != null

    override val definitions: Sequence<ScriptDefinition>
        get() {
            definitionsLock.read {
                if (_definitions != null) {
                    return _definitions!!.asSequence()
                }
            }

            forceStartUpdate = false
            asyncRunUpdateScriptTemplates()
            return emptySequence()
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

    protected abstract fun loadScriptDefinitions(previous: List<ScriptDefinition>?): List<ScriptDefinition>

    private var _definitions: List<ScriptDefinition>? = null
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
                    if (wasRunning && !forceStartUpdate && _definitions != null) return@write false
                    val newDefinitions = loadScriptDefinitions(_definitions)
                    if (newDefinitions != _definitions) {
                        _definitions = newDefinitions
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