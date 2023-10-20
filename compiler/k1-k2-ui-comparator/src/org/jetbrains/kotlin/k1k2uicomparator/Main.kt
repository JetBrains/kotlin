/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.k1k2uicomparator

import java.awt.*
import javax.swing.*

class UIComparatorFrame : JFrame() {
    companion object {
        const val TITLE = "K1/K2 UI Comparator"
        const val PANES_GAP = 10

        val INITIAL_SOURCE = """
            fun main() {
                println("Done")
            }
        """.trimIndent()
    }

    private val mainCodeEditor = codeEditorArea(INITIAL_SOURCE)
    private val leftCodeViewer = codeEditorArea().apply {
        isEditable = false
        caret = CaretWithoutVisibilityAdjustment()
    }
    private val rightCodeViewer = codeEditorArea().apply {
        isEditable = false
        caret = CaretWithoutVisibilityAdjustment()
    }

    private val scrollPaneSynchronizer = ScrollPaneSynchronizer()

    init {
        setTitle(TITLE)
        defaultCloseOperation = EXIT_ON_CLOSE
        setSize(1200, 600)

        contentPane.apply {
            layout = GridLayout(1, 3, PANES_GAP, PANES_GAP)

            add(scrollPaneSynchronizer.createScrollPaneFor(leftCodeViewer).withTitle("K1"))
            add(scrollPaneSynchronizer.createScrollPaneFor(mainCodeEditor).withTitle("Clear Source"))
            add(scrollPaneSynchronizer.createScrollPaneFor(rightCodeViewer).withTitle("K2"))

            (this as? JComponent)?.border = BorderFactory.createEmptyBorder(PANES_GAP, PANES_GAP, PANES_GAP, PANES_GAP)

            mainCodeEditor.document.addDocumentListener(SameActionDocumentListener {
                mainCodeChangeListeners.forEach { it() }
            })

            background = Color(247, 248, 250)
        }
    }

    private val mainCodeChangeListeners = mutableListOf<() -> Unit>()

    fun addMainCodeChangeListener(callback: () -> Unit) {
        mainCodeChangeListeners.add(callback)
    }

    val mainCode: String get() = mainCodeEditor.text

    fun setLeftCode(text: String) {
        leftCodeViewer.text = text
    }

    fun setRightCode(text: String) {
        rightCodeViewer.text = text
    }
}

fun recalculateSourceForK1(source: String) = source.replace("[a-zA-Z]".toRegex(), "a")
fun recalculateSourceForK2(source: String) = source.replace("[a-zA-Z]".toRegex(), "b")

fun main() = EventQueue.invokeLater {
    spawn(::UIComparatorFrame).apply {
        setLeftCode(recalculateSourceForK1(mainCode))
        setRightCode(recalculateSourceForK2(mainCode))

        addMainCodeChangeListener {
            setLeftCode(recalculateSourceForK1(mainCode))
            setRightCode(recalculateSourceForK2(mainCode))
        }
    }
}
