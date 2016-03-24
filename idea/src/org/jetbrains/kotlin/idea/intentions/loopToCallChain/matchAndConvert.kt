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

import com.intellij.openapi.util.Key
import org.jetbrains.kotlin.idea.analysis.analyzeInContext
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.intentions.loopToCallChain.result.FindAndAssignTransformation
import org.jetbrains.kotlin.idea.intentions.loopToCallChain.result.FindAndReturnTransformation
import org.jetbrains.kotlin.idea.intentions.loopToCallChain.sequence.FilterTransformation
import org.jetbrains.kotlin.idea.intentions.loopToCallChain.sequence.FlatMapTransformation
import org.jetbrains.kotlin.idea.intentions.loopToCallChain.sequence.MapTransformation
import org.jetbrains.kotlin.idea.intentions.negate
import org.jetbrains.kotlin.idea.util.CommentSaver
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfo
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.addToStdlib.check
import java.util.*

object MatcherRegistrar {
    val sequenceMatchers: Collection<SequenceTransformationMatcher> = listOf(
            FilterTransformation.Matcher,
            MapTransformation.Matcher,
            FlatMapTransformation.Matcher
    )

    val resultMatchers: Collection<ResultTransformationMatcher> = listOf(
            FindAndReturnTransformation.Matcher,
            FindAndAssignTransformation.Matcher
    )
}

fun match(loop: KtForExpression): ResultTransformationMatch? {
    val sequenceTransformations = ArrayList<SequenceTransformation>()
    var state = MatchingState(
            statements = listOf(loop.body ?: return null),
            workingVariable = loop.loopParameter ?: return null,
            indexVariable = null,
            loop = loop
    )

    MatchLoop@
    while (true) {
        val block = state.statements.singleOrNull() as? KtBlockExpression
        if (block != null) {
            state = state.copy(statements = block.statements)
        }

        for (matcher in MatcherRegistrar.resultMatchers) {
            val match = matcher.match(state)
            if (match != null) {
                sequenceTransformations.addAll(match.sequenceTransformations)
                val resultMatch = ResultTransformationMatch(match.resultTransformation, sequenceTransformations)
                return resultMatch.check { checkSmartCastsPreserved(loop, it) }
            }
        }

        for (matcher in MatcherRegistrar.sequenceMatchers) {
            val match = matcher.match(state)
            if (match != null) {
                sequenceTransformations.addAll(match.transformations)
                state = match.newState
                continue@MatchLoop
            }
        }

        return null
    }
}

//TODO: offer to use of .asSequence() as an option
fun convertLoop(loop: KtForExpression, matchResult: ResultTransformationMatch): KtExpression {
    val commentSaver = CommentSaver(matchResult.resultTransformation.commentSavingRange)

    var callChain = matchResult.generateCallChain(loop)

    val result = matchResult.resultTransformation.convertLoop(callChain)

    commentSaver.restore(matchResult.resultTransformation.commentRestoringRange)

    return result
}

private fun checkSmartCastsPreserved(loop: KtForExpression, matchResult: ResultTransformationMatch): Boolean {
    val bindingContext = loop.analyze(BodyResolveMode.FULL)

    val SMARTCAST_KEY = Key<KotlinType>("SMARTCAST_KEY")
    val IMPLICIT_RECEIVER_SMARTCAST_KEY = Key<KotlinType>("IMPLICIT_RECEIVER_SMARTCAST")

    var smartCastsFound = false
    try {
        loop.forEachDescendantOfType<KtExpression> { expression ->
            bindingContext[BindingContext.SMARTCAST, expression]?.let {
                expression.putCopyableUserData(SMARTCAST_KEY, it)
                smartCastsFound = true
            }

            bindingContext[BindingContext.IMPLICIT_RECEIVER_SMARTCAST, expression]?.let {
                expression.putCopyableUserData(IMPLICIT_RECEIVER_SMARTCAST_KEY, it)
                smartCastsFound = true
            }
        }

        if (!smartCastsFound) return true // optimization

        val callChain = matchResult.generateCallChain(loop)

        val resolutionScope = loop.getResolutionScope(bindingContext, loop.getResolutionFacade())
        val dataFlowInfo = bindingContext.getDataFlowInfo(loop)
        val newBindingContext = callChain.analyzeInContext(resolutionScope, loop, dataFlowInfo = dataFlowInfo)

        val smartCastBroken = callChain.anyDescendantOfType<KtExpression> { expression ->
            val smartCastType = expression.getCopyableUserData(SMARTCAST_KEY)
            if (smartCastType != null && newBindingContext[BindingContext.SMARTCAST, expression] != smartCastType && newBindingContext.getType(expression) != smartCastType) {
                return@anyDescendantOfType true
            }

            val implicitReceiverSmartCastType = expression.getCopyableUserData(IMPLICIT_RECEIVER_SMARTCAST_KEY)
            if (implicitReceiverSmartCastType != null && newBindingContext[BindingContext.IMPLICIT_RECEIVER_SMARTCAST, expression] != implicitReceiverSmartCastType) {
                return@anyDescendantOfType true
            }

            false
        }

        return !smartCastBroken
    }
    finally {
        if (smartCastsFound) {
            loop.forEachDescendantOfType<KtExpression> { it.putCopyableUserData(SMARTCAST_KEY, null) }
        }
    }
}

private fun ResultTransformationMatch.generateCallChain(loop: KtForExpression): KtExpression {
    var sequenceTransformations = sequenceTransformations
    val last = sequenceTransformations.lastOrNull()
    var lastFilter: FilterOrMap? = null
    var lastMap: FilterOrMap? = null //TODO
    if (last is FilterTransformation && resultTransformation.canIncludeFilter) {
        val condition = if (last.isInverse) last.condition.negate() else last.condition
        lastFilter = FilterOrMap(condition, last.inputVariable)
        sequenceTransformations = sequenceTransformations.take(sequenceTransformations.size - 1)
    }

    val lineBreak = if (sequenceTransformations.isNotEmpty()) "\n" else ""

    var callChain = loop.loopRange!!

    val psiFactory = KtPsiFactory(loop)
    fun chainedCallGenerator(): ChainedCallGenerator {
        return object : ChainedCallGenerator {
            override fun generate(pattern: String, vararg args: Any): KtExpression {
                val newPattern = "\$${args.size}$lineBreak.$pattern"
                return psiFactory.createExpressionByPattern(newPattern, *args, callChain)
            }
        }
    }

    for (transformation in sequenceTransformations) {
        callChain = transformation.generateCode(chainedCallGenerator())
    }

    callChain = resultTransformation.generateCode(chainedCallGenerator(), lastFilter, lastMap)
    return callChain
}

