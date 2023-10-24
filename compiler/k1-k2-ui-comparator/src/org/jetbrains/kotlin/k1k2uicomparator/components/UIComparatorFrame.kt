/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.k1k2uicomparator.components

import org.jetbrains.kotlin.k1k2uicomparator.support.*
import java.awt.Color
import java.awt.GridLayout
import javax.swing.JComponent
import javax.swing.JFrame

data class UIComparatorStyle(
    val title: String = "UI Comparator",
    val initialSource: String = DefaultStyles.DEFAULT_SOURCE,
    val leftViewerTitle: String = "Left Viewer",
    val mainViewerTitle: String = "Main Viewer",
    val rightViewerTitle: String = "Right Viewer",
    val panelsGapSize: Int = DefaultStyles.DEFAULT_GAP,
    val defaultWidth: Int = 1200,
    val defaultHeight: Int = 600,
    val background: Color = MoreColors.LIGHT_GRAY,
    val codeAreasStyle: CodeAreaStyle = CodeAreaStyle(),
)

class UIComparatorFrame(style: UIComparatorStyle = UIComparatorStyle()) : JFrame() {
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

    private val mainCodeEditor = codeArea(style.initialSource, style.codeAreasStyle)
    private val leftCodeViewer = codeArea("", style.codeAreasStyle, readonly = true)
    private val rightCodeViewer = codeArea("", style.codeAreasStyle, readonly = true)

    private val scrollPaneSynchronizer = ScrollPaneSynchronizer()

    init {
        setTitle(style.title)
        defaultCloseOperation = EXIT_ON_CLOSE
        setSize(style.defaultWidth, style.defaultHeight)

        contentPane.apply {
            layout = GridLayout(1, 3, style.panelsGapSize, style.panelsGapSize)

            +scrollPaneSynchronizer.createScrollPaneFor(leftCodeViewer).withTitle(style.leftViewerTitle, style.panelsGapSize)
            +scrollPaneSynchronizer.createScrollPaneFor(mainCodeEditor).withTitle(style.mainViewerTitle, style.panelsGapSize)
            +scrollPaneSynchronizer.createScrollPaneFor(rightCodeViewer).withTitle(style.rightViewerTitle, style.panelsGapSize)

            (this as? JComponent)?.border = emptyBorderWithEqualGaps(style.panelsGapSize)

            mainCodeEditor.document.addDocumentListener(SameActionDocumentListener {
                mainCodeChangeListeners.forEach { it() }
            })

            background = style.background
        }
    }
}
