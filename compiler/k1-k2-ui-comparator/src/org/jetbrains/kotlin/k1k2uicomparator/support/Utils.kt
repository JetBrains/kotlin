/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.k1k2uicomparator.support

import java.awt.*
import javax.swing.*

fun <T : JFrame> spawn(construct: () -> T) =
    construct().apply {
        setLocationRelativeTo(null)
        isVisible = true
    }

context(Container)
operator fun Component.unaryPlus(): Component = add(this)

data class ComponentWithConstraints(
    val component: Component,
    val constraints: Any,
)

fun <C : Any> Component.with(constraints: C, configureConstraints: C.() -> Unit = {}) =
    ComponentWithConstraints(this, constraints.apply(configureConstraints))

context(Container)
operator fun ComponentWithConstraints.unaryPlus(): Unit = add(component, constraints)

val JViewport.scrollableHeight get() = viewSize.height - extentSize.height
val JViewport.scrollableWidth get() = viewSize.width - extentSize.width
