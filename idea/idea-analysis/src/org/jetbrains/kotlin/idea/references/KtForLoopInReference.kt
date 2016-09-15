/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.references

import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.util.OperatorNameConventions

class KtForLoopInReference(element: KtForExpression) : KtMultiReference<KtForExpression>(element) {

    override fun getRangeInElement(): TextRange {
        val inKeyword = expression.inKeyword ?: return TextRange.EMPTY_RANGE

        val offset = inKeyword.startOffsetInParent
        return TextRange(offset, offset + inKeyword.textLength)
    }

    override fun getTargetDescriptors(context: BindingContext): Collection<DeclarationDescriptor> {
        val loopRange = expression.loopRange ?: return emptyList()
        return LOOP_RANGE_KEYS.mapNotNull { key -> context.get(key, loopRange)?.candidateDescriptor }
    }

    override val resolvesByNames: Collection<String>
        get() = NAMES

    companion object {
        private val LOOP_RANGE_KEYS = arrayOf(
                BindingContext.LOOP_RANGE_ITERATOR_RESOLVED_CALL,
                BindingContext.LOOP_RANGE_NEXT_RESOLVED_CALL,
                BindingContext.LOOP_RANGE_HAS_NEXT_RESOLVED_CALL
        )

        private val NAMES = listOf(
                OperatorNameConventions.ITERATOR.identifier,
                OperatorNameConventions.NEXT.identifier,
                OperatorNameConventions.HAS_NEXT.identifier
        )
    }
}
