/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions.loopToCallChain.sequence

import org.jetbrains.kotlin.idea.intentions.loopToCallChain.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*

class MapTransformation(
    override val loop: KtForExpression,
    val inputVariable: KtCallableDeclaration,
    val indexVariable: KtCallableDeclaration?,
    val mapping: KtExpression,
    val mapNotNull: Boolean
) : SequenceTransformation {

    private val functionName = if (indexVariable != null)
        if (mapNotNull) "mapIndexedNotNull" else "mapIndexed"
    else
        if (mapNotNull) "mapNotNull" else "map"

    override val affectsIndex: Boolean
        get() = mapNotNull

    override val presentation: String
        get() = "$functionName{}"

    override fun generateCode(chainedCallGenerator: ChainedCallGenerator): KtExpression {
        val lambda = generateLambda(inputVariable, indexVariable, mapping, chainedCallGenerator.reformat)
        return chainedCallGenerator.generate("$functionName$0:'{}'", lambda)
    }

    /**
     * Matches:
     *     for (...) {
     *         val v = ...
     *         ...
     *     }
     */
    object Matcher : TransformationMatcher {
        override val indexVariableAllowed: Boolean
            get() = true

        override val embeddedBreakOrContinuePossible: Boolean
            get() = true

        override fun match(state: MatchingState): TransformationMatch.Sequence? {
            val declaration = state.statements.firstOrNull() as? KtProperty ?: return null //TODO: support multi-variables
            val initializer = declaration.initializer ?: return null
            if (declaration.hasWriteUsages()) return null
            val restStatements = state.statements.drop(1)

            if (initializer is KtBinaryExpression && initializer.operationToken == KtTokens.ELVIS) {
                val continueExpression = initializer.right as? KtContinueExpression ?: return null
                if (continueExpression.targetLoop() != state.innerLoop) return null

                val mapping = initializer.left ?: return null
                if (mapping.containsEmbeddedBreakOrContinue()) return null

                val transformation = if (state.indexVariable != null && state.indexVariable.hasUsages(mapping))
                    MapTransformation(state.outerLoop, state.inputVariable, state.indexVariable, mapping, mapNotNull = true)
                else
                    MapTransformation(state.outerLoop, state.inputVariable, null, mapping, mapNotNull = true)
                val newState = state.copy(statements = restStatements, inputVariable = declaration)
                return TransformationMatch.Sequence(transformation, newState)
            }

            if (initializer.containsEmbeddedBreakOrContinue()) return null

            val transformation = if (state.indexVariable != null && state.indexVariable.hasUsages(initializer))
                MapTransformation(state.outerLoop, state.inputVariable, state.indexVariable, initializer, mapNotNull = false)
            else
                MapTransformation(state.outerLoop, state.inputVariable, null, initializer, mapNotNull = false)
            val newState = state.copy(statements = restStatements, inputVariable = declaration)
            return TransformationMatch.Sequence(transformation, newState)
        }
    }
}
