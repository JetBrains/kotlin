/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
package org.jetbrains.kotlin.idea.filters

import com.intellij.execution.filters.FileHyperlinkInfo
import com.intellij.execution.filters.HyperlinkInfoBase
import com.intellij.execution.filters.OpenFileHyperlinkInfo
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBList
import java.awt.Component
import javax.swing.JList
import javax.swing.ListCellRenderer

class InlineFunctionHyperLinkInfo(
        private val project: Project,
        private val inlineInfo: List<InlineInfo>
) : HyperlinkInfoBase(), FileHyperlinkInfo {

    override fun navigate(project: Project, hyperlinkLocationPoint: RelativePoint?) {
        if (inlineInfo.isEmpty()) return

        if (inlineInfo.size == 1) {
            OpenFileHyperlinkInfo(project, inlineInfo.first().file, inlineInfo.first().line).navigate(project)
        }
        else {
            val list = JBList(inlineInfo)
            list.cellRenderer = InlineInfoCellRenderer()
            val popup = JBPopupFactory.getInstance().createListPopupBuilder(list)
                    .setTitle("Navigate to")
                    .setItemChoosenCallback {
                        val fileInfo = list.selectedValue as InlineInfo
                        OpenFileHyperlinkInfo(project, fileInfo.file, fileInfo.line).navigate(project)
                    }
                    .createPopup()

            if (hyperlinkLocationPoint != null) {
                popup.show(hyperlinkLocationPoint)
            }
            else {
                popup.showInFocusCenter()
            }
        }
    }

    override fun getDescriptor(): OpenFileDescriptor? {
        val file = inlineInfo.firstOrNull()
        return file?.let { OpenFileDescriptor(project, file.file, file.line, 0) }
    }

    sealed class InlineInfo(val prefix: String, val file: VirtualFile, val line: Int) {
        class CallSiteInfo(file: VirtualFile, line: Int): InlineInfo("inline function call site", file, line)
        class InlineFunctionBodyInfo(file: VirtualFile, line: Int): InlineInfo("inline function body", file, line)
    }

    private class InlineInfoCellRenderer : SimpleColoredComponent(), ListCellRenderer<InlineInfo> {
        init {
            isOpaque = true
        }

        override fun getListCellRendererComponent(
                list: JList<out InlineInfo>?,
                value: InlineInfo?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean
        ): Component {

            clear()

            if (value != null) {
                append(value.prefix)
            }

            if (isSelected) {
                background = list?.selectionBackground
                foreground = list?.selectionForeground
            }
            else {
                background = list?.background
                foreground = list?.foreground
            }

            return this
        }
    }
}
