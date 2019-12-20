/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.highlighter.markers

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.util.ScalableIcon
import com.intellij.ui.LayeredIcon
import com.intellij.util.ui.ColorsIcon
import com.intellij.util.ui.JBUI
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.idea.highlighter.dsl.DslHighlighterExtension
import javax.swing.Icon

// BUNCH: as35
internal fun createDslStyleIcon(styleId: Int): Icon {
    val globalScheme = EditorColorsManager.getInstance().globalScheme
    val markersColor = globalScheme.getAttributes(DslHighlighterExtension.styleById(styleId)).foregroundColor
    val icon = LayeredIcon(2)
    val defaultIcon = KotlinIcons.DSL_MARKER_ANNOTATION
    icon.setIcon(defaultIcon, 0)
    icon.setIcon(
        (ColorsIcon(defaultIcon.iconHeight / 2, markersColor) as ScalableIcon).scale(JBUI.pixScale()),
        1,
        defaultIcon.iconHeight / 2,
        defaultIcon.iconWidth / 2
    )
    return icon
}