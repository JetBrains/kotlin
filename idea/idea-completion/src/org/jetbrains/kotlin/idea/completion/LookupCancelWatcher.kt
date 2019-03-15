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

package org.jetbrains.kotlin.idea.completion

import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupEvent
import com.intellij.codeInsight.lookup.LookupListener
import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import org.jetbrains.kotlin.idea.util.compat.EditorFactoryListenerWrapper

class LookupCancelWatcher(val project: Project) : ProjectComponent {
    private class Reminiscence(editor: Editor, offset: Int) {
        var editor: Editor? = editor
        private var marker: RangeMarker? = editor.document.createRangeMarker(offset, offset)

        // forget about auto-popup cancellation when the caret is moved to the start or before it
        private var editorListener: CaretListener? = object : CaretListener {
            override fun caretPositionChanged(e: CaretEvent) {
                if (!marker!!.isValid || editor.logicalPositionToOffset(e.newPosition) <= offset) {
                    dispose()
                }
            }
        }

        init {
            ApplicationManager.getApplication()!!.assertIsDispatchThread()
            editor.caretModel.addCaretListener(editorListener!!)
        }

        fun matches(editor: Editor, offset: Int): Boolean {
            return editor == this.editor && marker?.startOffset == offset
        }

        fun dispose() {
            ApplicationManager.getApplication()!!.assertIsDispatchThread()
            if (marker != null) {
                editor!!.caretModel.removeCaretListener(editorListener!!)
                marker = null
                editor = null
                editorListener = null
            }
        }
    }

    private var lastReminiscence: Reminiscence? = null

    companion object {
        fun getInstance(project: Project): LookupCancelWatcher = project.getComponent(LookupCancelWatcher::class.java)!!

        val AUTO_POPUP_AT = Key<Int>("LookupCancelWatcher.AUTO_POPUP_AT")
    }

    fun wasAutoPopupRecentlyCancelled(editor: Editor, offset: Int): Boolean {
        return lastReminiscence?.matches(editor, offset) ?: false
    }

    private val lookupCancelListener = object : LookupListener {
        override fun lookupCanceled(event: LookupEvent) {
            val lookup = event.lookup
            if (event.isCanceledExplicitly && lookup.isCompletion) {
                val offset = lookup.currentItem?.getUserData(AUTO_POPUP_AT)
                if (offset != null) {
                    lastReminiscence?.dispose()
                    if (offset <= lookup.editor.document.textLength) {
                        lastReminiscence = Reminiscence(lookup.editor, offset)
                    }
                }
            }
        }

        override fun itemSelected(event: LookupEvent) {
            // BUNCH: 182
            // Has default implementation since 183
        }

        override fun currentItemChanged(event: LookupEvent) {
            // BUNCH: 182
            // Has default implementation since 183
        }
    }

    override fun initComponent() {
        EditorFactory.getInstance().addEditorFactoryListener(
            object : EditorFactoryListenerWrapper {
                override fun editorReleased(event: EditorFactoryEvent) {
                    if (lastReminiscence?.editor == event.editor) {
                        lastReminiscence!!.dispose()
                    }
                }
            },
            project
        )

        LookupManager.getInstance(project).addPropertyChangeListener { event ->
            if (event.propertyName == LookupManager.PROP_ACTIVE_LOOKUP) {
                (event.newValue as Lookup?)?.addLookupListener(lookupCancelListener)
            }
        }
    }
}
