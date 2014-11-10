/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.references

import com.intellij.openapi.util.TextRange
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.lang.resolve.BindingContext
import java.util.Collections
import org.jetbrains.jet.lang.psi.JetForClause
import org.jetbrains.jet.lang.psi.JetForExpression
import org.jetbrains.jet.utils.addToStdlib.singletonOrEmptyList

public class JetForLoopInReference(element: JetForClause) : JetMultiReference<JetForClause>(element) {

    override fun getRangeInElement(): TextRange {
        val inKeywordNode = expression.getInKeywordNode()
        if (inKeywordNode == null)
            return TextRange.EMPTY_RANGE

        val offset = inKeywordNode.getPsi()!!.getStartOffsetInParent()
        return TextRange(offset, offset + inKeywordNode.getTextLength())
    }

    override fun getTargetDescriptors(context: BindingContext): Collection<DeclarationDescriptor> {
        if ((expression.getParent() as JetForExpression).isComprehension()) {
            return context[BindingContext.FOR_COMPREHENSION_RESOLVED_CALL, expression]?.getCandidateDescriptor().singletonOrEmptyList()
        }

        val loopRange = expression.getLoopRange()
        if (loopRange == null) {
            return Collections.emptyList()
        }
        return LOOP_RANGE_KEYS.map { key -> context.get(key, loopRange)?.getCandidateDescriptor() }.filterNotNull()
    }

    class object {
        private val LOOP_RANGE_KEYS = array(
                BindingContext.LOOP_RANGE_ITERATOR_RESOLVED_CALL,
                BindingContext.LOOP_RANGE_NEXT_RESOLVED_CALL,
                BindingContext.LOOP_RANGE_HAS_NEXT_RESOLVED_CALL
        )
    }
}
