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

package org.jetbrains.kotlin.idea.properties

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.lang.properties.references.PropertiesCompletionContributor
import com.intellij.lang.properties.references.PropertyReference
import org.jetbrains.kotlin.idea.completion.KotlinCompletionExtension
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isPlain
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

class PropertyKeyCompletion : KotlinCompletionExtension() {
    override fun perform(parameters: CompletionParameters, result: CompletionResultSet): Boolean {
        val template = parameters.position.getStrictParentOfType<KtStringTemplateExpression>() ?: return false
        if (!template.isPlain()) return false

        val references = template.references
        val propertyReference = references.firstIsInstanceOrNull<PropertyReference>() ?: return false
        if (PropertiesCompletionContributor.hasMoreImportantReference(references, propertyReference)) return false

        val startOffset = parameters.offset
        val offsetInElement = startOffset - template.startOffset
        val range = propertyReference.rangeInElement
        if (offsetInElement < range.startOffset) return false

        val prefix = template.text.substring(range.startOffset, offsetInElement)
        val variants = PropertiesCompletionContributor.getVariants(propertyReference)
        result.withPrefixMatcher(prefix).addAllElements(variants.toList())

        return true
    }
}
