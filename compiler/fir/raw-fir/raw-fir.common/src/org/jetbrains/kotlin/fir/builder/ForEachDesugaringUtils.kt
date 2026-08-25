/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.FirContractViolation
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirExpressionRef
import org.jetbrains.kotlin.fir.FirFunctionTarget
import org.jetbrains.kotlin.fir.FirLoopTarget
import org.jetbrains.kotlin.fir.buildWhenSubjectAccess
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.builder.buildProperty
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression
import org.jetbrains.kotlin.fir.expressions.buildBinaryArgumentList
import org.jetbrains.kotlin.fir.expressions.builder.FirBlockBuilder
import org.jetbrains.kotlin.fir.expressions.builder.buildElseIfTrueCondition
import org.jetbrains.kotlin.fir.expressions.builder.buildEqualityOperatorCall
import org.jetbrains.kotlin.fir.expressions.builder.buildLiteralExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildRegularWhenBranch
import org.jetbrains.kotlin.fir.expressions.builder.buildUnitExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildWhenExpression
import org.jetbrains.kotlin.fir.expressions.impl.buildSingleExpressionBlock
import org.jetbrains.kotlin.fir.symbols.impl.FirLocalPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImplWithoutSource
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.types.ConstantValueKind

fun FirBlockBuilder.generateForEachPrologue(scope: ForEachScope.Completed): Unit =
    scope.outerJumpableUses.forEach { use ->
        use.asLoopUse()?.let { loopUse ->
            loopUse.breakVariable?.let { statements += it }
            loopUse.continueVariable?.let { statements += it }
        }
        use.asFunctionUse()?.let { functionUse ->
            functionUse.resultVariable?.let { statements += it }
            functionUse.returnVariable.let { statements += it }
        }
    }

@OptIn(FirContractViolation::class)
fun FirBlockBuilder.generateForEachGuard(
    sourceElement: KtSourceElement,
    scope: ForEachScope.Completed,
    flagVariable: FirProperty,
    jumpDesugaringData: JumpDesugaringData,
) {
    val guardExpression = jumpDesugaringData.generateDesugaredJumpExpressionAsBlock(sourceElement)
    val syntheticSubject = buildProperty {
        source = sourceElement.fakeElement(KtFakeSourceElementKind.DesugaredForEachWhenGeneratedSubject(flagVariable.name))
        moduleData = scope.moduleData
        origin = FirDeclarationOrigin.Synthetic.ImplicitWhenSubject
        returnTypeRef = FirImplicitTypeRefImplWithoutSource
        name = SpecialNames.WHEN_SUBJECT
        initializer = generateResolvedAccessExpression(source, flagVariable)
        symbol = FirLocalPropertySymbol()
        isVar = false
        status = FirDeclarationStatusImpl(Visibilities.Local, Modality.FINAL)
        isLocal = true
    }
    val ref = FirExpressionRef<FirWhenExpression>()
    statements += buildWhenExpression {
        source = sourceElement
        subjectVariable = syntheticSubject
        branches += buildRegularWhenBranch {
            source = sourceElement
            condition = buildEqualityOperatorCall {
                source = sourceElement
                operation = FirOperation.EQ
                argumentList = buildBinaryArgumentList(
                    left = buildWhenSubjectAccess(source!!, this@buildWhenExpression.subjectVariable),
                    right = buildLiteralExpression(source, ConstantValueKind.Boolean, true, setType = true),
                )
            }
            result = guardExpression
        }
        // Exhaustiveness check requires this branch, but also we cannot emit diagnostics on this synthetic when-expression anyway,
        // since it does not have a proper source (which causes errors during diagnostic reporting)
        branches += buildRegularWhenBranch {
            source = sourceElement
            condition = buildElseIfTrueCondition { source = sourceElement }
            result = buildSingleExpressionBlock(
                statement = buildUnitExpression {
                    source = sourceElement.fakeElement(KtFakeSourceElementKind.ImplicitUnit.DesugaredForEachGuard)
                }
            )
        }
        usedAsExpression = false
    }.also(ref::bind)
}

fun FirBlockBuilder.generateForEachEpilogue(scope: ForEachScope.Completed) {
    val outerScope = scope.previousScope
    scope.outerJumpableUses.forEach { use ->
        use.asLoopUse()?.let { loopUse ->
            loopUse.asBreak()?.let {
                val sourceElement = source!!.fakeElement(KtFakeSourceElementKind.DesugaredForEachGuard.Break(it.breakSources))
                generateForEachGuard(
                    sourceElement = sourceElement,
                    scope = scope,
                    flagVariable = it.breakVariable,
                    jumpDesugaringData = outerScope.markBreak(it.position.eraseScope(scope), it.breakSources),
                )
            }
            loopUse.asContinue()?.let {
                val sourceElement = source!!.fakeElement(KtFakeSourceElementKind.DesugaredForEachGuard.Continue(it.continueSources))
                generateForEachGuard(
                    sourceElement = sourceElement,
                    scope = scope,
                    flagVariable = it.continueVariable,
                    jumpDesugaringData = outerScope.markContinue(it.position.eraseScope(scope), it.continueSources),
                )
            }
        }
        use.asFunctionUse()?.let {
            val sourceElement = source!!.fakeElement(KtFakeSourceElementKind.DesugaredForEachGuard.Return(it.returnSources))
            generateForEachGuard(
                sourceElement = sourceElement,
                scope = scope,
                flagVariable = it.returnVariable,
                jumpDesugaringData = outerScope.markReturn(
                    functionPosition = it.position.eraseScope(scope),
                    sources = it.returnSources,
                    resultExpression = it.resultVariable?.let { resultVariable ->
                        generateResolvedAccessExpression(source!!, resultVariable)
                    }
                ),
            )
        }
    }
}

inline fun AbstractRawFirBuilder<*>.desugarJumpExpression(
    labelName: String?,
    sourceElement: KtSourceElement,
    markJump: ForEachScope.(String?, KtSourceElement) -> JumpDesugaringData?,
    defaultExpression: () -> FirExpression
): FirExpression = context.currentForEachScope?.let { scope ->
    // If there is immediate forEach scope enclosing this expression, no desugaring is necessary
    if (scope.previousCompletedScope == null) return@let null
    // Consider only exising loop targets, i.e., fail silently so we do not interfere with other cone diagnostics
    scope.markJump(labelName, sourceElement)?.generateDesugaredJumpExpression(sourceElement)
} ?: defaultExpression()

inline fun <E : FirElement> AbstractRawFirBuilder<*>.desugarLoopTarget(target: FirLoopTarget, block: () -> E): E =
    context.currentForEachScope?.let { scope ->
        val target = LoopTarget.AnyLoop(target)
        scope += target
        val element = block()
        scope -= target
        element
    } ?: block()

inline fun <E> AbstractRawFirBuilder<*>.desugarFunctionTarget(
    target: FirFunctionTarget,
    resultTypeRef: FirTypeRef = FirImplicitTypeRefImplWithoutSource,
    block: () -> E
): E = context.currentForEachScope?.let { scope ->
    val target = FunctionTarget(target, resultTypeRef)
    scope += target
    val element = block()
    scope -= target
    element
} ?: block()

inline fun <E> AbstractRawFirBuilder<*>.desugarAnonymousFunctionTarget(
    target: FirFunctionTarget,
    resultTypeRef: FirTypeRef = FirImplicitTypeRefImplWithoutSource,
    block: () -> E
): Pair<E, Set<FirPropertySymbol>> = context.currentForEachScope?.let { scope ->
    val target = FunctionTarget(target, resultTypeRef, isAnonymousFunction = true)
    scope += target
    val element = block()
    scope -= target
    element to target.resultVariables
} ?: (block() to emptySet())
