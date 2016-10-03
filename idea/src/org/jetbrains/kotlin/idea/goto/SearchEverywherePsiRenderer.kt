/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.goto

import com.intellij.ide.util.DefaultPsiElementCellRenderer
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.presentation.java.SymbolPresentationUtil
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.renderer.ParameterNameRenderingPolicy
import java.util.*
import javax.swing.JList

class KotlinSearchEverywherePsiRenderer(private val list: JList<*>) : DefaultPsiElementCellRenderer() {
    private val RENDERER = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.withOptions {
        parameterNameRenderingPolicy = ParameterNameRenderingPolicy.NONE
        modifiers = emptySet()
        startFromName = false
    }

    override fun getElementText(element: PsiElement?): String {
        if (element is KtNamedFunction) {
            val descriptor = element.resolveToDescriptor() as FunctionDescriptor
            return buildString {
                descriptor.extensionReceiverParameter?.let { append(RENDERER.renderType(it.type)).append('.') }
                append(element.name)
                descriptor.valueParameters.joinTo(this, prefix = "(", postfix = ")") { RENDERER.renderType(it.type) }
            }
        }
        return super.getElementText(element)
    }

    // Mostly copied from SearchEverywherePsiRenderer
    override fun getContainerText(element: PsiElement?, name: String?): String? {
        var text = SymbolPresentationUtil.getSymbolContainerText(element) ?: return null
        if (list.width == 0) return text

        if (text.startsWith("(") && text.endsWith(")")) {
            text = text.substring(1, text.length - 1)
        }

        val inIndex = text.indexOf("in ")
        if (inIndex >= 0) text = text.substring(inIndex + 3)
        val fm = list.getFontMetrics(list.font)
        val maxWidth = list.width - fm.stringWidth(name) - 16 - myRightComponentWidth - 20
        val left = if (inIndex >= 0) "(in " else "("
        val right = ")"

        if (fm.stringWidth(left + text + right) < maxWidth) return left + text + right
        val parts = LinkedList(StringUtil.split(text, "."))
        var index: Int
        while (parts.size > 1) {
            index = parts.size / 2 - 1
            parts.removeAt(index)
            if (fm.stringWidth(StringUtil.join(parts, ".") + "...") < maxWidth) {
                parts.add(index, if (index == 0) "..." else ".")
                return left + StringUtil.join(parts, ".") + right
            }
        }

        return left + "..." + right
    }
}
