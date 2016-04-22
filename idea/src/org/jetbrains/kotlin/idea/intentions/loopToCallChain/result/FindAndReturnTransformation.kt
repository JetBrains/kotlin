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

package org.jetbrains.kotlin.idea.intentions.loopToCallChain.result

import org.jetbrains.kotlin.idea.intentions.loopToCallChain.*
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.psiUtil.PsiChildRange

class FindAndReturnTransformation(
        override val loop: KtForExpression,
        private val generator: FindOperatorGenerator,
        private val endReturn: KtReturnExpression
) : ResultTransformation {

    override val commentSavingRange = PsiChildRange(loop.unwrapIfLabeled(), endReturn)

    override val presentation: String
        get() = generator.presentation

    override val chainCallCount: Int
        get() = generator.chainCallCount

    override val shouldUseInputVariable: Boolean
        get() = generator.shouldUseInputVariable

    override fun generateCode(chainedCallGenerator: ChainedCallGenerator): KtExpression {
        return generator.generate(chainedCallGenerator)
    }

    override fun convertLoop(resultCallChain: KtExpression, commentSavingRangeHolder: CommentSavingRangeHolder): KtExpression {
        endReturn.returnedExpression!!.replace(resultCallChain)
        loop.deleteWithLabels()
        return endReturn
    }
}