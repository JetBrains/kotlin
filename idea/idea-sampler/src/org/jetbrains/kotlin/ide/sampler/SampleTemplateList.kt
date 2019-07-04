/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.sampler

import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.GrowPolicy
import com.intellij.ui.layout.panel
import javax.swing.JPanel
import javax.swing.JTextPane

class SampleTemplateList {
    lateinit var search: JBTextField
    lateinit var description: JTextPane
    lateinit var list: JBList<SampleInfo>
        private set

    fun setItems(items: List<SampleInfo>) {
        list.model = JBList.createDefaultListModel(items)
    }

    val mainPanel: JPanel =
        panel {
            row {
                cell {
                    JBTextField().apply { search = this }(growX, pushX)
                }
            }
            row {
                JBScrollPane(JBList<SampleInfo>().apply { list = this })(growX, pushX, growPolicy = GrowPolicy.MEDIUM_TEXT)
                JTextPane().apply {
                    description = this
                    isRequestFocusEnabled = true
                }(growX, pushX)
            }
        }

    init {
        list.addListSelectionListener {
            list.selectedValue?.let {
                description.text = assemblyText(it)
                description.caretPosition = 0
            }
        }
    }

    private fun assemblyText(item: SampleInfo): String {
        return StringBuilder()
            .append(item.description).append("\n")
            .append("Tags: ").append(item.tags).append("\n")
            .append("Libraries: ").append(item.libraries)
            .toString()
    }
}