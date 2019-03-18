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
import com.intellij.openapi.util.Ref
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.cfg.pseudocode.Pseudocode
import org.jetbrains.kotlin.cfg.pseudocode.PseudocodeUtil
import org.jetbrains.kotlin.cfg.pseudocode.containingDeclarationForPseudocode
import org.jetbrains.kotlin.idea.analysis.analyzeAsReplacement
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.loopToCallChain.result.*
import org.jetbrains.kotlin.idea.intentions.loopToCallChain.sequence.*
import org.jetbrains.kotlin.idea.project.builtIns
import org.jetbrains.kotlin.idea.util.CommentSaver
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils.isSubtypeOfClass
import org.jetbrains.kotlin.resolve.calls.smartcasts.ExplicitSmartCasts
import org.jetbrains.kotlin.resolve.calls.smartcasts.ImplicitSmartCasts
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

object MatcherRegistrar {
    val matchers: Collection<TransformationMatcher> = listOf(
            FindTransformationMatcher,
            AddToCollectionTransformation.Matcher,
            CountTransformation.Matcher,
            SumTransformationBase.Matcher,
            MaxOrMinTransformation.Matcher,
            IntroduceIndexMatcher,
            FilterTransformationBase.Matcher,
            MapTransformation.Matcher,
            FlatMapTransformation.Matcher,
            ForEachTransformation.Matcher
    )
}

data class MatchResult(
        val sequenceExpression: KtExpression,
        val transformationMatch: TransformationMatch.Result,
        val initializationStatementsToDelete: Collection<KtExpression>
)

//TODO: loop which is already over Sequence
fun match(loop: KtForExpression, useLazySequence: Boolean, reformat: Boolean): MatchResult? {
    val (inputVariable, indexVariable, sequenceExpression) = extractLoopData(loop) ?: return null

    var state = createInitialMatchingState(loop, inputVariable, indexVariable, useLazySequence, reformat) ?: return null

    // used just as optimization to avoid unnecessary checks
    val loopContainsEmbeddedBreakOrContinue = loop.containsEmbeddedBreakOrContinue()

    MatchLoop@
    while (true) {
        state = state.unwrapBlock()

        val inputVariableUsed = state.inputVariable.hasUsages(state.statements)

        // drop index variable if it's not used anymore
        if (state.indexVariable != null && !state.indexVariable!!.hasUsages(state.statements)) {
            state = state.copy(indexVariable = null)
        }

        val restContainsEmbeddedBreakOrContinue = loopContainsEmbeddedBreakOrContinue && state.statements.any { it.containsEmbeddedBreakOrContinue() }

        MatchersLoop@
        for (matcher in MatcherRegistrar.matchers) {
            if (state.indexVariable != null && !matcher.indexVariableAllowed) continue
            if (matcher.shouldUseInputVariables && !inputVariableUsed && state.indexVariable == null) continue

            val match = matcher.match(state)
            if (match != null) {
                when (match) {
                    is TransformationMatch.Sequence -> {
                        // check that old input variable is not needed anymore
                        var newState = match.newState
                        if (state.inputVariable != newState.inputVariable && state.inputVariable.hasUsages(newState.statements)) return null

                        if (matcher.shouldUseInputVariables
                            && !state.inputVariable.hasDifferentSetsOfUsages(state.statements, newState.statements)
                            && !(state.indexVariable?.hasDifferentSetsOfUsages(state.statements, newState.statements) ?: false)) {
                            // matched part of the loop uses neither input variable nor index variable
                            continue@MatchersLoop
                        }

                        if (state.indexVariable != null && match.sequenceTransformations.any { it.affectsIndex }) {
                            // index variable is still needed but index in the new sequence is different
                            if (state.indexVariable!!.hasUsages(newState.statements)) return null
                            newState = newState.copy(indexVariable = null)
                        }

                        if (restContainsEmbeddedBreakOrContinue && !matcher.embeddedBreakOrContinuePossible) {
                            val countBefore = state.statements.sumBy { it.countEmbeddedBreaksAndContinues() }
                            val countAfter = newState.statements.sumBy { it.countEmbeddedBreaksAndContinues() }
                            if (countAfter != countBefore) continue@MatchersLoop // some embedded break or continue in the matched part
                        }

                        state.previousTransformations += match.sequenceTransformations
                        state = newState
                        continue@MatchLoop
                    }

                    is TransformationMatch.Result -> {
                        if (restContainsEmbeddedBreakOrContinue && !matcher.embeddedBreakOrContinuePossible) continue@MatchersLoop

                        state.previousTransformations += match.sequenceTransformations

                        var result = TransformationMatch.Result(match.resultTransformation, state.previousTransformations)
                        result = mergeTransformations(result, reformat)

                        if (useLazySequence) {
                            val sequenceTransformations = result.sequenceTransformations
                            val resultTransformation = result.resultTransformation
                            if (sequenceTransformations.isEmpty() && !resultTransformation.lazyMakesSense
                                || sequenceTransformations.size == 1 && resultTransformation is AssignToListTransformation) {
                                return null // it makes no sense to use lazy sequence if no intermediate sequences produced
                            }
                            val asSequence = AsSequenceTransformation(loop)
                            result = TransformationMatch.Result(resultTransformation, listOf(asSequence) + sequenceTransformations)
                        }


                        return MatchResult(sequenceExpression, result, state.initializationStatementsToDelete)
                                .takeIf { checkSmartCastsPreserved(loop, it) }
                    }
                }
            }
        }

        return null
    }
}

fun convertLoop(loop: KtForExpression, matchResult: MatchResult): KtExpression {
    val resultTransformation = matchResult.transformationMatch.resultTransformation

    val commentSavingRange = resultTransformation.commentSavingRange
    val commentSaver = CommentSaver(commentSavingRange)
    val commentSavingRangeHolder = CommentSavingRangeHolder(commentSavingRange)

    matchResult.initializationStatementsToDelete.forEach { commentSavingRangeHolder.add(it) }

    val callChain = matchResult.generateCallChain(loop, true)

    commentSavingRangeHolder.remove(loop.unwrapIfLabeled()) // loop will be deleted in all cases
    val result = resultTransformation.convertLoop(callChain, commentSavingRangeHolder)
    commentSavingRangeHolder.add(result)

    for (statement in matchResult.initializationStatementsToDelete) {
        commentSavingRangeHolder.remove(statement)
        statement.delete()
    }

    // we need to adjust indent of the result because in some cases it's made incorrect when moving statement closer to the loop
    commentSaver.restore(commentSavingRangeHolder.range, forceAdjustIndent = true)

    return result
}

data class LoopData(
        val inputVariable: KtCallableDeclaration,
        val indexVariable: KtCallableDeclaration?,
        val sequenceExpression: KtExpression)

private fun extractLoopData(loop: KtForExpression): LoopData? {
    val loopRange = loop.loopRange ?: return null

    val destructuringParameter = loop.destructuringDeclaration
    if (destructuringParameter != null && destructuringParameter.entries.size == 2) {
        val qualifiedExpression = loopRange as? KtDotQualifiedExpression
        if (qualifiedExpression != null) {
            val call = qualifiedExpression.selectorExpression as? KtCallExpression
            if (call != null && call.calleeExpression.isSimpleName(Name.identifier("withIndex")) && call.valueArguments.isEmpty()) {
                val receiver = qualifiedExpression.receiverExpression
                if (!isExpressionTypeSupported(receiver)) return null
                return LoopData(destructuringParameter.entries[1], destructuringParameter.entries[0], receiver)
            }
        }
    }

    if (!isExpressionTypeSupported(loopRange)) return null

    return LoopData(loop.loopParameter ?: return null, null, loopRange)
}

private fun createInitialMatchingState(
        loop: KtForExpression,
        inputVariable: KtCallableDeclaration,
        indexVariable: KtCallableDeclaration?,
        useLazySequence: Boolean,
        reformat: Boolean
): MatchingState? {

    val pseudocodeProvider: () -> Pseudocode = object : () -> Pseudocode {
        val pseudocode: Pseudocode by lazy {
            val declaration = loop.containingDeclarationForPseudocode!!
            val bindingContext = loop.analyze(BodyResolveMode.FULL)
            PseudocodeUtil.generatePseudocode(declaration, bindingContext)
        }

        override fun invoke() = pseudocode
    }

    return MatchingState(
            outerLoop = loop,
            innerLoop = loop,
            statements = listOf(loop.body ?: return null),
            inputVariable = inputVariable,
            indexVariable = indexVariable,
            lazySequence = useLazySequence,
            pseudocodeProvider = pseudocodeProvider,
            reformat = reformat
    )
}

private fun isExpressionTypeSupported(expression: KtExpression): Boolean {
    val type = expression.analyze(BodyResolveMode.PARTIAL).getType(expression) ?: return false

    val builtIns = expression.builtIns
    return when {
        isSubtypeOfClass(type, builtIns.iterable) -> true
        isSubtypeOfClass(type, builtIns.array) -> true
        KotlinBuiltIns.isPrimitiveArray(type) -> true
        // TODO: support Sequence<T>
        else -> false
    }
}

private fun checkSmartCastsPreserved(loop: KtForExpression, matchResult: MatchResult): Boolean {
    val bindingContext = loop.analyze(BodyResolveMode.FULL)

    // we declare these keys locally to avoid possible race-condition problems if this code is executed in 2 threads simultaneously
    val SMARTCAST_KEY = Key<Ref<ExplicitSmartCasts>>("SMARTCAST_KEY")
    val IMPLICIT_RECEIVER_SMARTCAST_KEY = Key<Ref<ImplicitSmartCasts>>("IMPLICIT_RECEIVER_SMARTCAST")

    val storedUserData = mutableListOf<Ref<*>>()

    var smartCastCount = 0
    try {
        loop.forEachDescendantOfType<KtExpression> { expression ->
            bindingContext[BindingContext.SMARTCAST, expression]?.let { explicitSmartCasts ->
                Ref(explicitSmartCasts).apply {
                    expression.putCopyableUserData(SMARTCAST_KEY, this)
                    storedUserData += this
                }
                smartCastCount++
            }

            bindingContext[BindingContext.IMPLICIT_RECEIVER_SMARTCAST, expression]?.let { implicitSmartCasts ->
                Ref(implicitSmartCasts).apply {
                    expression.putCopyableUserData(IMPLICIT_RECEIVER_SMARTCAST_KEY, this)
                    storedUserData += this
                }
                smartCastCount++
            }
        }

        if (smartCastCount == 0) return true // optimization

        val callChain = matchResult.generateCallChain(loop, false)

        val newBindingContext = callChain.analyzeAsReplacement(loop, bindingContext)

        var preservedSmartCastCount = 0
        callChain.forEachDescendantOfType<KtExpression> { expression ->
            val smartCastType = expression.getCopyableUserData(SMARTCAST_KEY)?.get()
            if (smartCastType != null) {
                if (newBindingContext[BindingContext.SMARTCAST, expression] == smartCastType || newBindingContext.getType(expression) == smartCastType) {
                    preservedSmartCastCount++
                }
            }

            val implicitReceiverSmartCastType = expression.getCopyableUserData(IMPLICIT_RECEIVER_SMARTCAST_KEY)?.get()
            if (implicitReceiverSmartCastType != null) {
                if (newBindingContext[BindingContext.IMPLICIT_RECEIVER_SMARTCAST, expression] == implicitReceiverSmartCastType) {
                    preservedSmartCastCount++
                }
            }
        }

        if (preservedSmartCastCount == smartCastCount) return true

        // not all smart cast expressions has been found in the result or have the same type after conversion, perform more expensive check
        val expression = matchResult.transformationMatch.resultTransformation.generateExpressionToReplaceLoopAndCheckErrors(callChain)
        if (!tryChangeAndCheckErrors(loop) { it.replace(expression) }) return false

        return true
    }
    finally {
        storedUserData.forEach { it.set(null) }
        if (smartCastCount > 0) {
            loop.forEachDescendantOfType<KtExpression> {
                it.putCopyableUserData(SMARTCAST_KEY, null)
                it.putCopyableUserData(IMPLICIT_RECEIVER_SMARTCAST_KEY, null)
            }
        }
    }
}

private fun MatchResult.generateCallChain(loop: KtForExpression, reformat: Boolean): KtExpression {
    var sequenceTransformations = transformationMatch.sequenceTransformations
    var resultTransformation = transformationMatch.resultTransformation
    while(true) {
        val last = sequenceTransformations.lastOrNull() ?: break
        resultTransformation = resultTransformation.mergeWithPrevious(last, reformat) ?: break
        sequenceTransformations = sequenceTransformations.dropLast(1)
    }

    val chainCallCount = sequenceTransformations.sumBy { it.chainCallCount } + resultTransformation.chainCallCount
    val lineBreak = if (chainCallCount > 1) "\n" else ""

    var callChain = sequenceExpression

    val psiFactory = KtPsiFactory(loop)
    val chainedCallGenerator = object : ChainedCallGenerator {
        override val receiver: KtExpression
            get() = callChain

        override val reformat: Boolean
            get() = reformat

        override fun generate(pattern: String, vararg args: Any, receiver: KtExpression, safeCall: Boolean): KtExpression {
            val dot = if (safeCall) "?." else "."
            val newPattern = "$" + args.size + lineBreak + dot + pattern
            return psiFactory.createExpressionByPattern(newPattern, *args, receiver, reformat = reformat)
        }
    }

    for (transformation in sequenceTransformations) {
        callChain = transformation.generateCode(chainedCallGenerator)
    }

    callChain = resultTransformation.generateCode(chainedCallGenerator)
    return callChain
}

private fun mergeTransformations(match: TransformationMatch.Result, reformat: Boolean): TransformationMatch.Result {
    val transformations = (match.sequenceTransformations + match.resultTransformation).toMutableList()

    var anyChange: Boolean
    do {
        anyChange = false
        for (index in 0..transformations.lastIndex - 1) {
            val transformation = transformations[index] as SequenceTransformation
            val next = transformations[index + 1]
            val merged = next.mergeWithPrevious(transformation, reformat) ?: continue
            transformations[index] = merged
            transformations.removeAt(index + 1)
            anyChange = true
            break
        }
    } while(anyChange)

    @Suppress("UNCHECKED_CAST")
    return TransformationMatch.Result(transformations.last() as ResultTransformation, transformations.dropLast(1) as List<SequenceTransformation>)
}

data class IntroduceIndexData(
        val indexVariable: KtCallableDeclaration,
        val initializationStatement: KtExpression,
        val incrementExpression: KtUnaryExpression
)

fun matchIndexToIntroduce(loop: KtForExpression, reformat: Boolean): IntroduceIndexData? {
    if (loop.destructuringDeclaration != null) return null
    val (inputVariable, indexVariable) = extractLoopData(loop) ?: return null
    if (indexVariable != null) return null // loop is already with "withIndex"

    val state = createInitialMatchingState(loop, inputVariable, indexVariable, useLazySequence = false, reformat = reformat)?.unwrapBlock() ?: return null

    val match = IntroduceIndexMatcher.match(state) ?: return null
    assert(match.sequenceTransformations.isEmpty())
    val newState = match.newState

    val initializationStatement = newState.initializationStatementsToDelete.single()
    val incrementExpression = newState.incrementExpressions.single()
    return IntroduceIndexData(newState.indexVariable!!, initializationStatement, incrementExpression)
}
