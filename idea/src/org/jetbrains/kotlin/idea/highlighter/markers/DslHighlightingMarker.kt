/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.highlighter.markers

import com.intellij.application.options.colors.ColorAndFontOptions
import com.intellij.codeHighlighting.Pass
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.ide.DataManager
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import com.intellij.util.Function
import com.intellij.util.ui.ColorsIcon
import com.intellij.util.ui.JBUI
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.core.toDescriptor
import org.jetbrains.kotlin.idea.highlighter.dsl.DslHighlighterExtension
import org.jetbrains.kotlin.idea.highlighter.dsl.isDslHighlightingMarker
import org.jetbrains.kotlin.psi.KtClass
import javax.swing.Icon
import javax.swing.JComponent

private val navHandler = GutterIconNavigationHandler<PsiElement> { event, element ->
    val dataContext = (event.component as? JComponent)?.let { DataManager.getInstance().getDataContext(it) }
            ?: return@GutterIconNavigationHandler
    val ktClass = element?.parent as? KtClass ?: return@GutterIconNavigationHandler
    val styleId = ktClass.styleIdForMarkerAnnotation() ?: return@GutterIconNavigationHandler
    ColorAndFontOptions.selectOrEditColor(dataContext, DslHighlighterExtension.styleOptionDisplayName(styleId), KotlinLanguage.NAME)
}

private val toolTipHandler = Function<PsiElement, String> {
    "Marker annotation for DSL"
}

fun collectHighlightingColorsMarkers(
    ktClass: KtClass,
    result: MutableCollection<LineMarkerInfo<*>>
) {
    val styleId = ktClass.styleIdForMarkerAnnotation() ?: return

    val anchor = ktClass.nameIdentifier ?: return

    result.add(
        LineMarkerInfo<PsiElement>(
            anchor,
            anchor.textRange,
            createIcon(styleId),
            Pass.LINE_MARKERS,
            toolTipHandler, navHandler,
            GutterIconRenderer.Alignment.RIGHT
        )
    )
}

private fun createIcon(styleId: Int): Icon {
    val globalScheme = EditorColorsManager.getInstance().globalScheme
    val markersColor = globalScheme.getAttributes(DslHighlighterExtension.styleById(styleId)).foregroundColor
    val defaultColor = globalScheme.defaultForeground
    return JBUI.scale(ColorsIcon(12, markersColor, defaultColor, defaultColor, markersColor))
}

private fun KtClass.styleIdForMarkerAnnotation(): Int? {
    val classDescriptor = toDescriptor() as? ClassDescriptor ?: return null
    if (classDescriptor.kind != ClassKind.ANNOTATION_CLASS) return null
    if (!classDescriptor.isDslHighlightingMarker()) return null
    return DslHighlighterExtension.styleIdByMarkerAnnotation(classDescriptor)
}