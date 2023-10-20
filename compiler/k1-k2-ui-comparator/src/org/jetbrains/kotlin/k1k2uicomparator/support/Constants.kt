/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.k1k2uicomparator.support

import java.awt.Color
import javax.swing.border.LineBorder

object MoreColors {
    val TRANSPARENT = Color(0, 0, 0, 0)
    val LIGHT_GRAY = Color(247, 248, 250)
    val DARK_GRAY = Color(235, 236, 240)
}

object DefaultStyles {
    const val DEFAULT_GAP = 10
    const val DEFAULT_BORDER_THICKNESS = 1
    const val DEFAULT_FONT_SIZE = 14

    const val DEFAULT_SOURCE = "// Type here..."
}

object DefaultDecorations {
    val DEFAULT_BORDER = LineBorder(MoreColors.DARK_GRAY, DefaultStyles.DEFAULT_BORDER_THICKNESS, false)
}
