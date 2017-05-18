/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.slicer

import com.intellij.slicer.SliceUsage
import com.intellij.slicer.SliceUsageCellRendererBase
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.FontUtil
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.renderer.ParameterNameRenderingPolicy

// Based on com.intellij.slicer.SliceUsageCellRenderer
object KotlinSliceUsageCellRenderer : SliceUsageCellRendererBase() {
    private val descriptorRenderer = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.withOptions {
        withDefinedIn = true
        withoutTypeParameters = true
        parameterNameRenderingPolicy = ParameterNameRenderingPolicy.NONE
        includeAdditionalModifiers = false
        withoutSuperTypes = true
    }

    override fun customizeCellRendererFor(sliceUsage: SliceUsage) {
        if (sliceUsage !is KotlinSliceUsage) return

        for ((i, textChunk) in sliceUsage.getText().withIndex()) {
            append(textChunk.text, textChunk.simpleAttributesIgnoreBackground)
            if (i == 0) {
                append(FontUtil.spaceAndThinSpace())
            }
        }

        val declaration = sliceUsage.element?.parents?.firstOrNull {
            it is KtClass ||
            it is KtObjectDeclaration && !it.isObjectLiteral() ||
            it is KtDeclarationWithBody ||
            it is KtProperty && it.isLocal
        } as? KtDeclaration ?: return
        val descriptor = declaration.resolveToDescriptor()
        append(" in ${descriptorRenderer.render(descriptor)}", SimpleTextAttributes.GRAY_ATTRIBUTES)
    }
}