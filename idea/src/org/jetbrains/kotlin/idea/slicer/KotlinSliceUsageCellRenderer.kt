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
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.renderer.ClassifierNamePolicy
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.ParameterNameRenderingPolicy
import org.jetbrains.kotlin.resolve.descriptorUtil.isCompanionObject
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension

// Based on com.intellij.slicer.SliceUsageCellRenderer
object KotlinSliceUsageCellRenderer : SliceUsageCellRendererBase() {
    private val descriptorRenderer = DescriptorRenderer.ONLY_NAMES_WITH_SHORT_TYPES.withOptions {
        withoutReturnType = true
        renderConstructorKeyword = false
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
            it is KtClass ||
                    it is KtObjectDeclaration && !it.isObjectLiteral() ||
                    it is KtNamedFunction && !it.isLocal ||
                    it is KtProperty && !it.isLocal ||
                    it is KtConstructor<*>
        } as? KtDeclaration ?: return

        append(" in ", SimpleTextAttributes.GRAY_ATTRIBUTES)

        val descriptor = declaration.unsafeResolveToDescriptor()

        if (!descriptor.isExtension && descriptor !is ConstructorDescriptor && !descriptor.isCompanionObject()) {
            val containingClassifier = descriptor.containingDeclaration as? ClassifierDescriptor
            if (containingClassifier != null) {
                append(descriptorRenderer.render(containingClassifier), SimpleTextAttributes.GRAY_ATTRIBUTES)
                append(".", SimpleTextAttributes.GRAY_ATTRIBUTES)
            }
        }

        append(descriptorRenderer.render(descriptor), SimpleTextAttributes.GRAY_ATTRIBUTES)
    }
}