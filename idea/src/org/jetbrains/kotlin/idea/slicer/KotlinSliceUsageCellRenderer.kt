/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.slicer

import com.intellij.slicer.SliceUsage
import com.intellij.slicer.SliceUsageCellRendererBase
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.FontUtil
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.renderer.ParameterNameRenderingPolicy

// Based on com.intellij.slicer.SliceUsageCellRenderer
object KotlinSliceUsageCellRenderer : SliceUsageCellRendererBase() {
    private val descriptorRenderer = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_NO_ANNOTATIONS.withOptions {
        withDefinedIn = true
        withoutTypeParameters = true
        parameterNameRenderingPolicy = ParameterNameRenderingPolicy.NONE
        includeAdditionalModifiers = false
        withoutSuperTypes = true
    }

    override fun customizeCellRendererFor(sliceUsage: SliceUsage) {
        if (sliceUsage !is KotlinSliceUsage) return
        val isDereference = sliceUsage is KotlinSliceDereferenceUsage

        for ((i, textChunk) in sliceUsage.getText().withIndex()) {
            var attributes = textChunk.simpleAttributesIgnoreBackground
            if (isDereference) {
                attributes = attributes.derive(attributes.style, JBColor.LIGHT_GRAY, attributes.bgColor, attributes.waveColor)
            }

            append(textChunk.text, attributes)
            if (i == 0) {
                append(FontUtil.spaceAndThinSpace())
            }
        }

        append(" (Tracking enclosing lambda)".repeat(sliceUsage.lambdaLevel), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)

        val declaration = sliceUsage.element?.parents?.firstOrNull {
            it is KtClass || it is KtObjectDeclaration && !it.isObjectLiteral() || it is KtDeclarationWithBody || it is KtProperty && it.isLocal
        } as? KtDeclaration ?: return
        val descriptor = declaration.unsafeResolveToDescriptor()
        append(" in ${descriptorRenderer.render(descriptor)}", SimpleTextAttributes.GRAY_ATTRIBUTES)
    }
}