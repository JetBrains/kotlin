/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.k1k2uicomparator

import org.jetbrains.kotlin.k1k2uicomparator.components.UIComparatorFrame
import org.jetbrains.kotlin.k1k2uicomparator.support.spawn
import java.awt.EventQueue

fun replaceWithA(source: String) = source.replace("[a-zA-Z]".toRegex(), "a")
fun replaceWithB(source: String) = source.replace("[a-zA-Z]".toRegex(), "b")

fun main() = EventQueue.invokeLater {
    spawn(::UIComparatorFrame).apply {
        setLeftCode(replaceWithA(mainCode))
        setRightCode(replaceWithB(mainCode))

        addMainCodeChangeListener {
            setLeftCode(replaceWithA(mainCode))
            setRightCode(replaceWithB(mainCode))
        }
    }
}
