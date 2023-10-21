/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.k1k2uicomparator.support

import java.awt.Color
import java.awt.Component
import java.awt.Point
import javax.swing.JScrollPane
import javax.swing.border.Border

class ScrollPaneSynchronizer {
    fun createScrollPaneFor(
        component: Component,
        background: Color = Color.WHITE,
        border: Border = DefaultDecorations.DEFAULT_BORDER,
    ) = JScrollPane(component).apply {
        scrollPanes += this
        scrollPanesCurrentPercentages[this] = ScrollPaneStateInfo(0, 0)

        // Viewport is changed before the scroll bars,
        // so be sure to not look at scrollBar's values.
        viewport.addChangeListener {
            applyScrollsFrom(this)
        }

        this.background = background
        this.border = border
    }

    private data class ScrollPaneStateInfo(
        var latestX: Int,
        var latestY: Int,
    ) {
        override fun toString() = "($latestX, $latestY)"
    }

    private val scrollPanes = mutableListOf<JScrollPane>()
    private val scrollPanesCurrentPercentages = mutableMapOf<JScrollPane, ScrollPaneStateInfo>()

    private fun applyScrollsFrom(scrollPane: JScrollPane) {
        val currentPosition = scrollPane.viewport.viewPosition

        if (currentPosition.x != scrollPanesCurrentPercentages[scrollPane]?.latestX) {
            scrollPanesCurrentPercentages[scrollPane]?.latestX = currentPosition.x
            updateHorizontalScrollPanesOtherThan(scrollPane, currentPosition.x)
        }

        if (currentPosition.y != scrollPanesCurrentPercentages[scrollPane]?.latestY) {
            scrollPanesCurrentPercentages[scrollPane]?.latestY = currentPosition.y
            updateVerticalScrollPanesOtherThan(scrollPane, currentPosition.y)
        }
    }

    private fun updateHorizontalScrollPanesOtherThan(that: JScrollPane, thatPosition: Int) {
        updateScrollPanesOtherThan(
            that, thatPosition,
            measureScrollableSpace = { it.viewport.scrollableWidth },
            getPosition = { it.viewport.viewPosition.x },
            setPosition = { it, newScrollBarValue ->
                scrollPanesCurrentPercentages[it]?.latestX = newScrollBarValue
                // Accessing `y` from the newer position, because it's orthogonal
                it.viewport.viewPosition = Point(newScrollBarValue, it.viewport.viewPosition.y)
            },
        )
    }

    private fun updateVerticalScrollPanesOtherThan(that: JScrollPane, thatPosition: Int) {
        updateScrollPanesOtherThan(
            that, thatPosition,
            measureScrollableSpace = { it.viewport.scrollableHeight },
            getPosition = { it.viewport.viewPosition.y },
            setPosition = { it, newScrollBarValue ->
                scrollPanesCurrentPercentages[it]?.latestY = newScrollBarValue
                // Accessing `x` from the newer position, because it's orthogonal
                it.viewport.viewPosition = Point(it.viewport.viewPosition.x, newScrollBarValue)
            },
        )
    }

    private inline fun updateScrollPanesOtherThan(
        that: JScrollPane,
        thatPosition: Int,
        measureScrollableSpace: (JScrollPane) -> Int,
        getPosition: (JScrollPane) -> Int,
        setPosition: (JScrollPane, Int) -> Unit,
    ) {
        val scrollableSpace = measureScrollableSpace(that)

        // We don't want the small viewport to reset the
        // bigger ones to 0 on scroll.
        if (scrollableSpace <= 0) {
            return
        }

        val scrollPercentage = thatPosition.toDouble() / scrollableSpace

        for (it in scrollPanes) {
            if (it == that) {
                // This is crucial, despite the cache.
                // After recalculating the position from
                // the current percentage we may get a different
                // value than the current one due to int rounding.
                continue
            }

            val newScrollBarValue = (scrollPercentage * measureScrollableSpace(it)).toInt()
            val itPosition = getPosition(it)

            if (newScrollBarValue != itPosition) {
                setPosition(it, newScrollBarValue)
            }
        }
    }
}
