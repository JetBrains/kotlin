/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.k1k2uicomparator

import javax.swing.JFrame
import javax.swing.JViewport

val JViewport.scrollableHeight get() = viewSize.height - extentSize.height
val JViewport.scrollableWidth get() = viewSize.width - extentSize.width

fun spawn(construct: () -> JFrame) =
    construct().apply {
        setLocationRelativeTo(null)
        isVisible = true
    }
