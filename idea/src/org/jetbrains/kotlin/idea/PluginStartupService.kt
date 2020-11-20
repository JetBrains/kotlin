/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.psi.search.searches.IndexPatternSearch
import org.jetbrains.kotlin.idea.search.ideaExtensions.KotlinTodoSearcher
import org.jetbrains.kotlin.idea.util.application.getServiceSafe

class PluginStartupService : Disposable {

    fun register(project: Project) {
        val eventMulticaster = EditorFactory.getInstance().eventMulticaster
        val documentListener: DocumentListener = object : DocumentListener {
            override fun documentChanged(e: DocumentEvent) {
                FileDocumentManager.getInstance().getFile(e.document)?.let { virtualFile ->
                    if (virtualFile.fileType === KotlinFileType.INSTANCE) {
                        KotlinPluginUpdater.getInstance().kotlinFileEdited(virtualFile)
                    }
                }
            }
        }
        eventMulticaster.addDocumentListener(documentListener, this)

        val indexPatternSearch = ServiceManager.getService(IndexPatternSearch::class.java)
        val kotlinTodoSearcher = KotlinTodoSearcher()
        indexPatternSearch.registerExecutor(kotlinTodoSearcher)

        Disposer.register(this, {
            eventMulticaster.removeDocumentListener(documentListener)
            indexPatternSearch.unregisterExecutor(kotlinTodoSearcher)
        })
    }

    override fun dispose() {
    }

    companion object {
        fun getInstance(project: Project): PluginStartupService = project.getServiceSafe()
    }
}