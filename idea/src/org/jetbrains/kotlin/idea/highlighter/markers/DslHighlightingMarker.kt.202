/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.highlighter.markers

import com.intellij.application.options.colors.ColorAndFontOptions
import com.intellij.codeHighlighting.Pass
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.ide.DataManager
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import com.intellij.util.Function
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.core.toDescriptor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.highlighter.dsl.DslHighlighterExtension
import org.jetbrains.kotlin.idea.highlighter.dsl.isDslHighlightingMarker
import org.jetbrains.kotlin.psi.KtClass
import javax.swing.JComponent

private val navHandler = GutterIconNavigationHandler<PsiElement> { event, element ->
    val dataContext = (event.component as? JComponent)?.let { DataManager.getInstance().getDataContext(it) }
        ?: return@GutterIconNavigationHandler
    val ktClass = element?.parent as? KtClass ?: return@GutterIconNavigationHandler
    val styleId = ktClass.styleIdForMarkerAnnotation() ?: return@GutterIconNavigationHandler
    ColorAndFontOptions.selectOrEditColor(dataContext, DslHighlighterExtension.styleOptionDisplayName(styleId), KotlinLanguage.NAME)
}

private val toolTipHandler = Function<PsiElement, String> {
    KotlinBundle.message("highlighter.tool.tip.marker.annotation.for.dsl")
}

fun collectHighlightingColorsMarkers(
    ktClass: KtClass,
    result: MutableCollection<in LineMarkerInfo<*>>
) {
    if (!KotlinLineMarkerOptions.dslOption.isEnabled) return

    val styleId = ktClass.styleIdForMarkerAnnotation() ?: return

    val anchor = ktClass.nameIdentifier ?: return

    result.add(
        LineMarkerInfo<PsiElement>(
            anchor,
            anchor.textRange,
            createDslStyleIcon(styleId),
            Pass.LINE_MARKERS,
            toolTipHandler, navHandler,
            GutterIconRenderer.Alignment.RIGHT
        )
    )
}

private fun KtClass.styleIdForMarkerAnnotation(): Int? {
    val classDescriptor = toDescriptor() as? ClassDescriptor ?: return null
    if (classDescriptor.kind != ClassKind.ANNOTATION_CLASS) return null
    if (!classDescriptor.isDslHighlightingMarker()) return null
    return DslHighlighterExtension.styleIdByMarkerAnnotation(classDescriptor)
}