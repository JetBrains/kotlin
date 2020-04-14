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
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.descriptorUtil.isCompanionObject
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import java.awt.Font

// Based on com.intellij.slicer.SliceUsageCellRenderer
object KotlinSliceUsageCellRenderer : SliceUsageCellRendererBase() {
    private val descriptorRenderer = DescriptorRenderer.ONLY_NAMES_WITH_SHORT_TYPES.withOptions {
        withoutReturnType = true
        renderConstructorKeyword = false
        valueParametersHandler = TruncatedValueParametersHandler(maxParameters = 2)
    }

    override fun customizeCellRendererFor(sliceUsage: SliceUsage) {
        if (sliceUsage !is KotlinSliceUsage) return
        val isDereference = sliceUsage is KotlinSliceDereferenceUsage

        for ((i, textChunk) in sliceUsage.getText().withIndex()) {
            var attributes = textChunk.simpleAttributesIgnoreBackground
            if (isDereference) {
                attributes = attributes.derive(attributes.style, JBColor.LIGHT_GRAY, attributes.bgColor, attributes.waveColor)
            }

            if (attributes.fontStyle == Font.BOLD) {
                attributes = attributes.derive(attributes.style or SimpleTextAttributes.STYLE_UNDERLINE, null, null, null)
            }

            append(textChunk.text, attributes)
            if (i == 0) {
                append(FontUtil.spaceAndThinSpace())
            }
        }

        for (behaviour in sliceUsage.mode.behaviourStack.reversed()) {
            append(behaviour.slicePresentationPrefix, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
        }

        containerSuffix(sliceUsage)?.let {
            append(" ")
            append(it, SimpleTextAttributes.GRAY_ATTRIBUTES)
        }
    }

    fun containerSuffix(sliceUsage: SliceUsage): String? {
        val element = sliceUsage.element ?: return null
        var declaration = element.parents.firstOrNull {
            it is KtClass ||
                    it is KtObjectDeclaration && !it.isObjectLiteral() ||
                    it is KtNamedFunction && !it.isLocal ||
                    it is KtProperty && !it.isLocal ||
                    it is KtPropertyAccessor ||
                    it is KtConstructor<*>
        } as? KtDeclaration ?: return null

        // for a val or var among primary constructor parameters show the class as container
        if (declaration is KtPrimaryConstructor && element is KtParameter && element.hasValOrVar()) {
            declaration = declaration.containingClassOrObject!!
        }

        return buildString {
            append(KotlinBundle.message("slicer.text.in", ""))
            append(" ")

            val descriptor = declaration.unsafeResolveToDescriptor()

            if (!descriptor.isExtension && descriptor !is ConstructorDescriptor && !descriptor.isCompanionObject()) {
                val containingClassifier = descriptor.containingDeclaration as? ClassifierDescriptor
                if (containingClassifier != null) {
                    append(descriptorRenderer.render(containingClassifier))
                    append(".")
                }
            }

            when (descriptor) {
                is PropertyDescriptor -> {
                    renderPropertyOrAccessor(descriptor)
                }

                is PropertyAccessorDescriptor -> {
                    val property = descriptor.correspondingProperty
                    renderPropertyOrAccessor(property, if (descriptor is PropertyGetterDescriptor) ".get" else ".set")
                }

                else -> {
                    append(descriptorRenderer.render(descriptor))
                }
            }
        }
    }

    private fun StringBuilder.renderPropertyOrAccessor(propertyDescriptor: PropertyDescriptor, accessorSuffix: String = "") {
        append(propertyDescriptor.name.render())
        append(accessorSuffix)
        val receiverType = propertyDescriptor.extensionReceiverParameter?.type
        if (receiverType != null) {
            append(" on ")
            append(descriptorRenderer.renderType(receiverType))
        }
    }

    private class TruncatedValueParametersHandler(private val maxParameters: Int) : DescriptorRenderer.ValueParametersHandler {
        private var truncateLength = -1

        override fun appendBeforeValueParameters(parameterCount: Int, builder: StringBuilder) {
            builder.append("(")
        }

        override fun appendAfterValueParameters(parameterCount: Int, builder: StringBuilder) {
            if (parameterCount > maxParameters) {
                builder.setLength(truncateLength)
                builder.append(",${Typography.ellipsis}")
            }
            builder.append(")")
        }

        override fun appendBeforeValueParameter(
            parameter: ValueParameterDescriptor,
            parameterIndex: Int,
            parameterCount: Int,
            builder: StringBuilder
        ) {
        }

        override fun appendAfterValueParameter(
            parameter: ValueParameterDescriptor,
            parameterIndex: Int,
            parameterCount: Int,
            builder: StringBuilder
        ) {
            if (parameterIndex < parameterCount - 1) {
                if (parameterIndex == maxParameters - 1) {
                    truncateLength = builder.length
                } else {
                    builder.append(", ")
                }
            }
        }
    }
}