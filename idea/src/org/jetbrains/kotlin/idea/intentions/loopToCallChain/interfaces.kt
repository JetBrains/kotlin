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

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.psiUtil.PsiChildRange
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.psi.psiUtil.siblings

/**
 * An abstraction for generating a chained call that knows about receiver expression and handles proper formatting
 */
interface ChainedCallGenerator {
    val receiver: KtExpression

    /**
     * @param pattern pattern string for generating the part of the call to the right from the dot
     */
    fun generate(pattern: String, vararg args: Any, receiver: KtExpression = this.receiver, safeCall: Boolean = false): KtExpression
}

/**
 * Base interface for recognized transformations of the sequence. Should always be either [SequenceTransformation] or [ResultTransformation]
 */
interface Transformation {
    val loop: KtForExpression

    val presentation: String

    fun buildPresentation(prevTransformationsPresentation: String?): String {
        return if (prevTransformationsPresentation != null)
            prevTransformationsPresentation + "." + presentation
        else
            presentation
    }

    fun generateCode(chainedCallGenerator: ChainedCallGenerator): KtExpression

    val chainCallCount: Int
        get() = 1

    val shouldUseInputVariable: Boolean
        get() = true
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

    /**
     * Implementations of this method are obliged to update [commentSavingRangeHolder] when deleting or adding any element into the tree
     * except for the loop itself and the result element returned from this method
     */
    fun convertLoop(resultCallChain: KtExpression, commentSavingRangeHolder: CommentSavingRangeHolder): KtExpression
}

/**
 * Represents a state when matching a part of the loop against known transformations
 */
data class MatchingState(
        val outerLoop: KtForExpression,
        val innerLoop: KtForExpression,
        val statements: List<KtExpression>,
        val inputVariable: KtCallableDeclaration,
        /**
         * Matchers can assume that indexVariable is null if it's not used in the rest of the loop
         */
        val indexVariable: KtCallableDeclaration?,
        val initializationStatementsToDelete: Collection<KtExpression> = emptyList()
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
    constructor(transformation: SequenceTransformation, newState: MatchingState) : this(listOf(transformation), newState)
}

/**
 * A matcher that can recognize a [ResultTransformation] (optionally prepended by some [SequenceTransformation]'s).
 * Should match the whole rest part of the loop.
 */
interface ResultTransformationMatcher {
    fun match(state: MatchingState): ResultTransformationMatch?

    val indexVariableUsePossible: Boolean
}

class ResultTransformationMatch(
        val resultTransformation: ResultTransformation,
        val sequenceTransformations: List<SequenceTransformation>
) {
    constructor(resultTransformation: ResultTransformation, vararg sequenceTransformations: SequenceTransformation)
    : this(resultTransformation, sequenceTransformations.asList())
}

/**
 * Helper class for holding and updating PsiChildRange to be used for [CommentSaver.restore] call
 */
class CommentSavingRangeHolder(range: PsiChildRange) {
    var range = range
        private set

    /**
     * Call this method when a new element to be included into the range is added into the tree
     */
    fun add(element: PsiElement) {
        if (range.isEmpty) {
            range = PsiChildRange.singleElement(element)
            return
        }

        val rangeParent = range.first!!.parent
        val elementToAdd = element.parentsWithSelf.takeWhile { it != rangeParent }.last()
        when (elementToAdd) {
            in range -> return

            in range.first!!.siblingsBefore() -> range = PsiChildRange(elementToAdd, range.last)

            else -> range = PsiChildRange(range.first, elementToAdd)
        }
    }

    /**
     * Call this method before deletion of any element which can belong to the range
     */
    fun remove(element: PsiElement) {
        when {
            range.isEmpty -> return

            element == range.first -> {
                val newFirst = element
                        .siblings(forward = true, withItself = false)
                        .takeWhile { it != range.last!!.nextSibling }
                        .firstOrNull { it !is PsiWhiteSpace }
                if (newFirst != null) {
                    range = PsiChildRange(newFirst, range.last)
                }
                else {
                    range = PsiChildRange.EMPTY
                }
            }

            element == range.last -> {
                val newLast = element
                        .siblings(forward = false, withItself = false)
                        .takeWhile { it != range.first!!.prevSibling }
                        .firstOrNull { it !is PsiWhiteSpace }
                if (newLast != null) {
                    range = PsiChildRange(range.first, newLast)
                }
                else {
                    range = PsiChildRange.EMPTY
                }
            }
        }
    }

    private fun PsiElement.siblingsBefore() = if (prevSibling != null) PsiChildRange(parent.firstChild, prevSibling) else PsiChildRange.EMPTY
}
