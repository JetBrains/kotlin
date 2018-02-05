/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.highlighter.dsl

import com.intellij.ide.highlighter.custom.CustomHighlighterColors.*
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.highlighter.HighlighterExtension
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.calls.DslMarkerUtils
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import kotlin.math.absoluteValue

class DslHighlighterExtension : HighlighterExtension() {
    override fun highlightDeclaration(elementToHighlight: PsiElement, descriptor: DeclarationDescriptor): TextAttributesKey? {
        return null
    }

    override fun highlightCall(elementToHighlight: PsiElement, resolvedCall: ResolvedCall<*>): TextAttributesKey? {
        val markerAnnotationFqName = markerAnnotationFqName(resolvedCall) ?: return null
        return styleForDsl(markerAnnotationFqName)
    }

    private fun markerAnnotationFqName(resolvedCall: ResolvedCall<*>): FqName? {
        val markerAnnotation = resolvedCall.resultingDescriptor.annotations.find { annotation ->
            annotation.annotationClass?.isDslHighlightingMarker() ?: false
        }

        return markerAnnotation?.fqName
    }

    companion object {
        private const val numStyles = 4

        private val defaultKeys = listOf(
            CUSTOM_KEYWORD1_ATTRIBUTES,
            CUSTOM_KEYWORD2_ATTRIBUTES,
            CUSTOM_KEYWORD3_ATTRIBUTES,
            CUSTOM_KEYWORD4_ATTRIBUTES
        )

        private val styles = (1..numStyles).map { index ->
            TextAttributesKey.createTextAttributesKey(externalKeyName(index), defaultKeys[index - 1])
        }

        fun externalKeyName(index: Int) = "KOTLIN_DSL_STYLE$index"

        val descriptionsToStyles = (1..numStyles).associate { index ->
            "Dsl//Style$index" to styles[index - 1]
        }

        private fun styleForDsl(markerAnnotationFqName: FqName) =
            styles[(markerAnnotationFqName.asString().hashCode() % numStyles).absoluteValue]
    }
}

internal fun ClassDescriptor.isDslHighlightingMarker(): Boolean {
    return annotations.any {
        it.annotationClass?.fqNameSafe == DslMarkerUtils.DSL_MARKER_FQ_NAME
    }
}
