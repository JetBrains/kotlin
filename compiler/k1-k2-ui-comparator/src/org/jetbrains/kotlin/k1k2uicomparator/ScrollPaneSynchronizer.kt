/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.k1k2uicomparator

import java.awt.Color
import java.awt.Component
import java.awt.Point
import javax.swing.JScrollPane
import javax.swing.border.LineBorder

class ScrollPaneSynchronizer {
    private data class ScrollPaneStateInfo(
        var latestX: Int,
        var latestY: Int,
    ) {
        override fun toString() = "($latestX, $latestY)"
    }

    private val scrollPanesByComponent = mutableMapOf<Component, JScrollPane>()
    private val scrollPanesCurrentPercentages = mutableMapOf<JScrollPane, ScrollPaneStateInfo>()

    private fun updateHorizontalScrollPanesOtherThan(that: JScrollPane) {
        val scrollableWidth = that.viewport.scrollableWidth

        // We don't want the small viewport to reset the
        // bigger ones to 0 on scroll.
        if (scrollableWidth <= 0) {
            return
        }

        val scrollPercentage = that.viewport.viewPosition.x.toDouble() / scrollableWidth

        for (it in scrollPanesByComponent.values) {
            val newScrollBarValue = (scrollPercentage * it.viewport.scrollableWidth).toInt()

            if (newScrollBarValue != it.viewport.viewPosition.x) {
                scrollPanesCurrentPercentages[it]?.latestX = newScrollBarValue
                it.viewport.viewPosition = Point(newScrollBarValue, it.viewport.viewPosition.y)
            }
        }
    }

    private fun updateVerticalScrollPanesOtherThan(that: JScrollPane) {
        val scrollableHeight = that.viewport.scrollableHeight

        // We don't want the small viewport to reset the
        // bigger ones to 0 on scroll.
        if (scrollableHeight <= 0) {
            return
        }

        val scrollPercentage = that.viewport.viewPosition.y.toDouble() / scrollableHeight

        for (it in scrollPanesByComponent.values) {
            val newScrollBarValue = (scrollPercentage * it.viewport.scrollableHeight).toInt()

            if (newScrollBarValue != it.viewport.viewPosition.y) {
                scrollPanesCurrentPercentages[it]?.latestY = newScrollBarValue
                it.viewport.viewPosition = Point(it.viewport.viewPosition.x, newScrollBarValue)
            }
        }
    }

    private fun applyScrollsFrom(scrollPane: JScrollPane) {
        if (scrollPane.viewport.viewPosition.x != scrollPanesCurrentPercentages[scrollPane]?.latestX) {
            scrollPanesCurrentPercentages[scrollPane]?.latestX = scrollPane.viewport.viewPosition.x
            updateHorizontalScrollPanesOtherThan(scrollPane)
        }

        if (scrollPane.viewport.viewPosition.y != scrollPanesCurrentPercentages[scrollPane]?.latestY) {
            scrollPanesCurrentPercentages[scrollPane]?.latestY = scrollPane.viewport.viewPosition.y
            updateVerticalScrollPanesOtherThan(scrollPane)
        }
    }

    fun createScrollPaneFor(component: Component) =
        JScrollPane(component).apply {
            scrollPanesByComponent[component] = this
            scrollPanesCurrentPercentages[this] = ScrollPaneStateInfo(0, 0)

            // Viewport is changed before the scroll bars,
            // so be sure to not look at scrollBar's values.
            viewport.addChangeListener {
                applyScrollsFrom(this)
            }

            border = LineBorder(Color(235, 236, 240), 1, false)
        }
}
