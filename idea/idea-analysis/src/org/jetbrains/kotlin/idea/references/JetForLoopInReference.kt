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
import org.jetbrains.kotlin.psi.JetForExpression
import org.jetbrains.kotlin.resolve.BindingContext
import java.util.Collections

public class JetForLoopInReference(element: JetForExpression) : JetMultiReference<JetForExpression>(element) {

    override fun getRangeInElement(): TextRange {
        val inKeywordNode = expression.getInKeywordNode()
        if (inKeywordNode == null)
            return TextRange.EMPTY_RANGE

        val offset = inKeywordNode.getPsi()!!.getStartOffsetInParent()
        return TextRange(offset, offset + inKeywordNode.getTextLength())
    }

    override fun getTargetDescriptors(context: BindingContext): Collection<DeclarationDescriptor> {
        val loopRange = expression.getLoopRange()
        if (loopRange == null) {
            return Collections.emptyList()
        }
        return LOOP_RANGE_KEYS.map { key -> context.get(key, loopRange)?.getCandidateDescriptor() }.filterNotNull()
    }

    default object {
        private val LOOP_RANGE_KEYS = array(
                BindingContext.LOOP_RANGE_ITERATOR_RESOLVED_CALL,
                BindingContext.LOOP_RANGE_NEXT_RESOLVED_CALL,
                BindingContext.LOOP_RANGE_HAS_NEXT_RESOLVED_CALL
        )
    }
}
