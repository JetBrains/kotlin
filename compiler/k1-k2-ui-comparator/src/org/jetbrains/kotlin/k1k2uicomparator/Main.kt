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

    val mainCodeEditor = codeEditorArea(INITIAL_SOURCE)
    val k1CodeViewer = codeEditorArea().apply {
        isEditable = false
//        document = mainCodeEditor.document
        // TODO: slow
        text = recalculateSourceForK1(INITIAL_SOURCE)
        caret = CaretWithoutVisibilityAdjustment()
    }
    val k2CodeViewer = codeEditorArea().apply {
        isEditable = false
//        document = mainCodeEditor.document
        text = recalculateSourceForK2(INITIAL_SOURCE)
        caret = CaretWithoutVisibilityAdjustment()
    }

    val scrollPaneSynchronizer = ScrollPaneSynchronizer()

    init {
        initialize()
    }
}

fun UIComparatorFrame.initialize() {
    setTitle(UIComparatorFrame.TITLE)
    defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    setSize(1200, 600)

    contentPane.apply {
        layout = GridLayout(1, 3, UIComparatorFrame.PANES_GAP, UIComparatorFrame.PANES_GAP)
        add(scrollPaneSynchronizer.createScrollPaneFor(k1CodeViewer))
        add(scrollPaneSynchronizer.createScrollPaneFor(mainCodeEditor))
        add(scrollPaneSynchronizer.createScrollPaneFor(k2CodeViewer))

        (this as? JComponent)?.border = BorderFactory.createEmptyBorder(
            UIComparatorFrame.PANES_GAP,
            UIComparatorFrame.PANES_GAP,
            UIComparatorFrame.PANES_GAP,
            UIComparatorFrame.PANES_GAP
        )

        mainCodeEditor.document.addDocumentListener(SameActionDocumentListener {
            k1CodeViewer.text = recalculateSourceForK1(mainCodeEditor.text)
            k2CodeViewer.text = recalculateSourceForK2(mainCodeEditor.text)
        })

        background = Color(247, 248, 250)
    }
}

fun recalculateSourceForK1(source: String) = source.replace("[a-zA-Z]".toRegex(), "a")
fun recalculateSourceForK2(source: String) = source.replace("[a-zA-Z]".toRegex(), "b")

fun main() {
    EventQueue.invokeLater {
        spawn(::UIComparatorFrame)
    }
}
