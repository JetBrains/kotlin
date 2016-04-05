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

package org.jetbrains.kotlin.idea.intentions.loopToCallChain

import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.psiUtil.PsiChildRange

interface ChainedCallGenerator {
    fun generate(pattern: String, vararg args: Any): KtExpression
}

interface Transformation {
    val inputVariable: KtCallableDeclaration
}

interface SequenceTransformation : Transformation {
    val affectsIndex: Boolean

    fun generateCode(chainedCallGenerator: ChainedCallGenerator): KtExpression
}

interface ResultTransformation : Transformation {
    fun mergeWithPrevious(previousTransformation: SequenceTransformation): ResultTransformation? = null

    val commentSavingRange: PsiChildRange
    val commentRestoringRange: PsiChildRange

    fun generateCode(chainedCallGenerator: ChainedCallGenerator): KtExpression

    fun convertLoop(resultCallChain: KtExpression): KtExpression
}

data class MatchingState(
        val outerLoop: KtForExpression,
        val innerLoop: KtForExpression,
        val statements: Collection<KtExpression>,
        val workingVariable: KtCallableDeclaration,
        val indexVariable: KtCallableDeclaration?
)

interface SequenceTransformationMatcher {
    fun match(state: MatchingState): SequenceTransformationMatch?
}

class SequenceTransformationMatch(
        val transformations: List<SequenceTransformation>,
        val newState: MatchingState
) {
    init {
        assert(transformations.isNotEmpty())
    }

    constructor(transformation: SequenceTransformation, newState: MatchingState) : this(listOf(transformation), newState)
}

interface ResultTransformationMatcher {
    fun match(state: MatchingState): ResultTransformationMatch?
}

class ResultTransformationMatch(
        val resultTransformation: ResultTransformation,
        val sequenceTransformations: List<SequenceTransformation> = listOf()
)
