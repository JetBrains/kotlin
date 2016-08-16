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

package org.jetbrains.kotlin.idea.intentions.loopToCallChain.sequence

import org.jetbrains.kotlin.idea.intentions.loopToCallChain.ChainedCallGenerator
import org.jetbrains.kotlin.idea.intentions.loopToCallChain.SequenceTransformation
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtForExpression

class AsSequenceTransformation(override val loop: KtForExpression) : SequenceTransformation {
    override val presentation: String
        get() = "asSequence()"

    override val affectsIndex: Boolean
        get() = false

    override fun generateCode(chainedCallGenerator: ChainedCallGenerator): KtExpression {
        return chainedCallGenerator.generate("asSequence()")
    }
}