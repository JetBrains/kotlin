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

/**
 * An abstraction for generating a chained call that knows about receiver expression and handles proper formatting
 */
interface ChainedCallGenerator {
    val receiver: KtExpression

    /**
     * @param pattern pattern string for generating the part of the call to the right from the dot
     */
    fun generate(pattern: String, vararg args: Any): KtExpression
}

/**
 * Base interface for recognized transformations of the sequence. Should always be either [SequenceTransformation] or [ResultTransformation]
 */
interface Transformation {
    val inputVariable: KtCallableDeclaration
    val loop: KtForExpression

    fun generateCode(chainedCallGenerator: ChainedCallGenerator): KtExpression
}

/**
 * Represents a transformation of input sequence into another sequence
 */
interface SequenceTransformation : Transformation {
    fun mergeWithPrevious(previousTransformation: SequenceTransformation): SequenceTransformation? = null

    val affectsIndex: Boolean

}

/**
 * Represents a final transformation of sequence which produces the result of the whole loop (for example, assigning a found value into a variable).
 */
interface ResultTransformation : Transformation {
    fun mergeWithPrevious(previousTransformation: SequenceTransformation): ResultTransformation? = null

    val commentSavingRange: PsiChildRange
    fun commentRestoringRange(convertLoopResult: KtExpression): PsiChildRange

    fun convertLoop(resultCallChain: KtExpression): KtExpression
}

/**
 * Represents a state when matching a part of the loop against known transformations
 */
data class MatchingState(
        val outerLoop: KtForExpression,
        val innerLoop: KtForExpression,
        val statements: Collection<KtExpression>,
        val inputVariable: KtCallableDeclaration,
        val indexVariable: KtCallableDeclaration?
)

/**
 * A matcher that can recognize one or more [SequenceTransformation]'s
 */
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

/**
 * A matcher that can recognize a [ResultTransformation] (optionally prepended by some [SequenceTransformation]'s).
 * Should match the whole rest part of the loop.
 */
interface ResultTransformationMatcher {
    fun match(state: MatchingState): ResultTransformationMatch?
}

class ResultTransformationMatch(
        val resultTransformation: ResultTransformation,
        val sequenceTransformations: List<SequenceTransformation>
) {
    constructor(resultTransformation: ResultTransformation, vararg sequenceTransformations: SequenceTransformation)
    : this(resultTransformation, sequenceTransformations.asList())
}
