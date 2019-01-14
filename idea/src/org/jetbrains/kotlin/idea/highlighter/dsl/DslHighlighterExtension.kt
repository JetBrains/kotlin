/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.highlighter.dsl

import com.intellij.ide.highlighter.custom.CustomHighlighterColors.*
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.highlighter.HighlighterExtension
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
        return dslCustomTextStyle(resolvedCall.resultingDescriptor)
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
            "Dsl//${styleOptionDisplayName(index)}" to styleById(index)
        }

        fun styleOptionDisplayName(index: Int) = "Style$index"

        fun styleIdByMarkerAnnotation(markerAnnotation: ClassDescriptor): Int? {
            val markerAnnotationFqName = markerAnnotation.fqNameSafe
            return (markerAnnotationFqName.asString().hashCode() % numStyles).absoluteValue + 1
        }

        fun dslCustomTextStyle(callableDescriptor: CallableDescriptor): TextAttributesKey? {
            val markerAnnotation = callableDescriptor.annotations.find { annotation ->
                annotation.annotationClass?.isDslHighlightingMarker() ?: false
            }?.annotationClass ?: return null

            val styleId = styleIdByMarkerAnnotation(markerAnnotation) ?: return null
            return styleById(styleId)
        }

        fun styleById(styleId: Int): TextAttributesKey = styles[styleId - 1]
    }
}

internal fun ClassDescriptor.isDslHighlightingMarker(): Boolean {
    return annotations.any {
        it.annotationClass?.fqNameSafe == DslMarkerUtils.DSL_MARKER_FQ_NAME
    }
}
