/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.FirAbstractTarget
import org.jetbrains.kotlin.fir.FirFunctionTarget
import org.jetbrains.kotlin.fir.FirLoopTarget
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.StandardTypes
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.builder.buildProperty
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.diagnostics.ConeForEachDesugaringDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.ConeForEachExpectedAnyLoop
import org.jetbrains.kotlin.fir.diagnostics.ConeForEachTargetDoesNotExist
import org.jetbrains.kotlin.fir.diagnostics.ConeForEachUnexpectedTargetInInnermostScope
import org.jetbrains.kotlin.fir.diagnostics.ConeForEachUnknownTarget
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.builder.FirBlockBuilder
import org.jetbrains.kotlin.fir.expressions.builder.buildBlock
import org.jetbrains.kotlin.fir.expressions.builder.buildBreakExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildContinueExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildErrorExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildLiteralExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildReturnExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildUnitExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildVariableAssignment
import org.jetbrains.kotlin.fir.expressions.impl.buildSingleExpressionBlock
import org.jetbrains.kotlin.fir.symbols.impl.FirLocalPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneTypeOrNull
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImplWithoutSource
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.ConstantValueKind
import java.util.Deque
import java.util.LinkedList

sealed interface JumpableTarget<T : FirAbstractTarget<*>> {

    val firTarget: T

    val labelName: String? get() = firTarget.labelName
}

sealed interface LoopTarget : JumpableTarget<FirAbstractTarget<*>> {

    fun asForEach(): ForEach?

    fun asAnyLoop(): AnyLoop?

    data class ForEach(override val firTarget: FirFunctionTarget) : LoopTarget {
        override fun asForEach(): ForEach = this
        override fun asAnyLoop(): AnyLoop? = null
    }

    data class AnyLoop(override val firTarget: FirLoopTarget) : LoopTarget {
        override fun asForEach(): ForEach? = null
        override fun asAnyLoop(): AnyLoop = this
    }
}

data class FunctionTarget(
    override val firTarget: FirFunctionTarget,
    val resultTypeRef: FirTypeRef = FirImplicitTypeRefImplWithoutSource,
    val isAnonymousFunction: Boolean = false,
) : JumpableTarget<FirFunctionTarget> {
    val resultVariables: Set<FirPropertySymbol>
        field = mutableSetOf()

    operator fun plusAssign(variable: FirPropertySymbol) {
        resultVariables += variable
    }
}

sealed interface JumpableUse<P : JumpablePosition<*>> {

    val position: P

    fun asLoopUse(): LoopUse?

    fun asFunctionUse(): FunctionUse?
}

sealed interface LoopUse : JumpableUse<LoopPosition> {

    override val position: LoopPosition

    val breakVariable: FirProperty? get() = null

    val breakSources: Set<KtSourceElement> get() = emptySet()

    val continueVariable: FirProperty? get() = null

    val continueSources: Set<KtSourceElement> get() = emptySet()

    fun markBreak(breakVariable: FirProperty, sourceElement: KtSourceElement?): Break =
        markBreak(breakVariable, sourceElement?.let(::setOf) ?: emptySet())

    fun markBreak(breakVariable: FirProperty, sources: Set<KtSourceElement>): Break

    fun markContinue(continueVariable: FirProperty, sourceElement: KtSourceElement?): Continue =
        markContinue(continueVariable, sourceElement?.let(::setOf) ?: emptySet())

    fun markContinue(continueVariable: FirProperty, sources: Set<KtSourceElement>): Continue

    fun asBreak(): Break?

    fun asContinue(): Continue?

    override fun asLoopUse(): LoopUse? = this

    override fun asFunctionUse(): FunctionUse? = null

//    data class NotReferenced(override val position: LoopPosition) : LoopUse {
//        override fun markBreak(breakVariable: FirProperty, sourceElement: KtSourceElement?): Break =
//            BreakOnly(position, breakVariable, sourceElement?.let { setOf(it) } ?: emptySet())
//
//        override fun markContinue(continueVariable: FirProperty, sourceElement: KtSourceElement?): Continue =
//            ContinueOnly(position, continueVariable, sourceElement?.let { setOf(it) } ?: emptySet())
//
//        override fun asBreak(): Break? = null
//        override fun asContinue(): Continue? = null
//    }

    sealed interface Break : LoopUse {
        override val breakVariable: FirProperty
    }

    data class BreakOnly(
        override val position: LoopPosition,
        override val breakVariable: FirProperty,
        override val breakSources: Set<KtSourceElement> = emptySet(),
    ) : Break {
        override fun markBreak(breakVariable: FirProperty, sources: Set<KtSourceElement>): Break =
            BreakOnly(position, breakVariable, breakSources + sources)

        override fun markContinue(continueVariable: FirProperty, sources: Set<KtSourceElement>): Continue =
            BreakAndContinue(position, breakVariable, breakSources, continueVariable, sources)

        override fun asBreak(): Break = this
        override fun asContinue(): Continue? = null
    }

    sealed interface Continue : LoopUse {
        override val continueVariable: FirProperty
    }

    data class ContinueOnly(
        override val position: LoopPosition,
        override val continueVariable: FirProperty,
        override val continueSources: Set<KtSourceElement>
    ) : Continue {
        override fun markBreak(breakVariable: FirProperty, sources: Set<KtSourceElement>): Break =
            BreakAndContinue(position, breakVariable, sources, continueVariable, continueSources)

        override fun markContinue(continueVariable: FirProperty, sources: Set<KtSourceElement>): Continue =
            ContinueOnly(position, continueVariable, continueSources + sources)

        override fun asBreak(): Break? = null
        override fun asContinue(): Continue = this
    }

    data class BreakAndContinue(
        override val position: LoopPosition,
        override val breakVariable: FirProperty,
        override val breakSources: Set<KtSourceElement> = emptySet(),
        override val continueVariable: FirProperty,
        override val continueSources: Set<KtSourceElement> = emptySet(),
    ) : Break, Continue {
        override fun markBreak(breakVariable: FirProperty, sources: Set<KtSourceElement>): Break =
            BreakAndContinue(
                position = position,
                breakVariable = breakVariable,
                breakSources = breakSources + sources,
                continueVariable = continueVariable,
                continueSources = continueSources,
            )

        override fun markContinue(continueVariable: FirProperty, sources: Set<KtSourceElement>): Continue =
            BreakAndContinue(
                position = position,
                breakVariable = breakVariable,
                breakSources = breakSources,
                continueVariable = continueVariable,
                continueSources = continueSources + sources,
            )

        override fun asBreak(): Break = this
        override fun asContinue(): Continue = this
    }
}

data class FunctionUse(
    override val position: FunctionPosition,
    val resultVariable: FirProperty?,
    val returnVariable: FirProperty,
    val returnSources: Set<KtSourceElement> = emptySet(),
) : JumpableUse<FunctionPosition> {
    fun markReturn(resultVariable: FirProperty?, returnVariable: FirProperty, sources: Set<KtSourceElement>): FunctionUse =
        FunctionUse(position, resultVariable, returnVariable, returnSources + sources)

    override fun asLoopUse(): LoopUse? = null

    override fun asFunctionUse(): FunctionUse = this
}

data class JumpablePosition<T : JumpableTarget<*>>(val scope: ForEachScope.Completed? = null, val target: T)

fun <T : JumpableTarget<*>> JumpablePosition<T>.eraseScope(scope: ForEachScope.Completed): JumpablePosition<T> =
    if (this.scope == scope) JumpablePosition(target = target) else this

typealias LoopPosition = JumpablePosition<LoopTarget>

typealias FunctionPosition = JumpablePosition<FunctionTarget>

sealed interface JumpDesugaringKind<U : JumpableUse<*>> {

    fun buildDesugaredExpression(sourceElement: KtSourceElement, scope: ForEachScope.Completed, jumpableUse: U): FirBlock =
        buildBlock {
            source = sourceElement
            buildDesugaredStatements(sourceElement, jumpableUse)
            // Desugaring of any jump expression always has to break the iteration of the innermost `forEach` loop
            statements += buildReturnExpression {
                source = sourceElement
                target = scope.target.firTarget
                result = buildLiteralExpression(sourceElement, ConstantValueKind.Boolean, false, setType = true)
            }
        }

    fun FirBlockBuilder.buildDesugaredStatements(sourceElement: KtSourceElement, jumpableUse: U)

    sealed class WithFlag<U : JumpableUse<*>> : JumpDesugaringKind<U> {

        inline fun FirBlockBuilder.buildFlagAssignment(sourceElement: KtSourceElement, jumpableUse: U, flagVariable: (U) -> FirProperty) {
            statements += buildVariableAssignment {
                source = sourceElement
                lValue = generateResolvedAccessExpression(sourceElement, flagVariable(jumpableUse))
                rValue = buildLiteralExpression(sourceElement, ConstantValueKind.Boolean, true, setType = true)
            }
        }
    }

    data class ResultAndReturnFlag(val resultExpression: FirExpression?) : WithFlag<FunctionUse>() {
        override fun FirBlockBuilder.buildDesugaredStatements(sourceElement: KtSourceElement, jumpableUse: FunctionUse) {
            jumpableUse.resultVariable?.let { resultVariable ->
                statements += buildVariableAssignment {
                    source = sourceElement
                    lValue = generateResolvedAccessExpression(sourceElement, resultVariable)
                    rValue = resultExpression ?: return@let
                }
            }
            buildFlagAssignment(sourceElement, jumpableUse, FunctionUse::returnVariable)
        }
    }

    data object BreakFlag : WithFlag<LoopUse.Break>() {
        override fun FirBlockBuilder.buildDesugaredStatements(sourceElement: KtSourceElement, jumpableUse: LoopUse.Break): Unit =
            buildFlagAssignment(sourceElement, jumpableUse, LoopUse.Break::breakVariable)
    }

    data object ContinueFlag : WithFlag<LoopUse.Continue>() {
        override fun FirBlockBuilder.buildDesugaredStatements(sourceElement: KtSourceElement, jumpableUse: LoopUse.Continue): Unit =
            buildFlagAssignment(sourceElement, jumpableUse, LoopUse.Continue::continueVariable)
    }
}

sealed interface JumpDesugaringData {

    fun generateDesugaredJumpExpression(sourceElement: KtSourceElement): FirExpression

    fun generateDesugaredJumpExpressionAsBlock(sourceElement: KtSourceElement): FirBlock

    fun interface DefaultDesugaringOrNone : JumpDesugaringData {
        override fun generateDesugaredJumpExpressionAsBlock(sourceElement: KtSourceElement): FirBlock =
            buildSingleExpressionBlock(statement = generateDesugaredJumpExpression(sourceElement))
    }

    data class Error(val errorDiagnostic: ConeForEachDesugaringDiagnostic) : JumpDesugaringData {
        override fun generateDesugaredJumpExpression(sourceElement: KtSourceElement): FirExpression =
            buildErrorExpression {
                source = sourceElement
                diagnostic = errorDiagnostic
            }

        override fun generateDesugaredJumpExpressionAsBlock(sourceElement: KtSourceElement): FirBlock =
            buildSingleExpressionBlock(statement = generateDesugaredJumpExpression(sourceElement))
    }

    data class DesugaringGivenUse<U : JumpableUse<*>>(
        val belongingScope: ForEachScope.Completed,
        val jumpableUse: U,
        val desugaringKind: JumpDesugaringKind<U>
    ) : JumpDesugaringData {
        override fun generateDesugaredJumpExpression(sourceElement: KtSourceElement): FirExpression =
            desugaringKind.buildDesugaredExpression(sourceElement, belongingScope, jumpableUse)

        override fun generateDesugaredJumpExpressionAsBlock(sourceElement: KtSourceElement): FirBlock =
            desugaringKind.buildDesugaredExpression(sourceElement, belongingScope, jumpableUse)
    }
}

sealed interface ForEachScope : Set<JumpableTarget<*>> {

    val previousScope: ForEachScope?

    val previousCompletedScope: Completed?

    fun findLoop(name: String?, nextCompleted: Completed? = null): LoopPosition?

    fun findFunction(name: String?, nextCompleted: Completed? = null): FunctionPosition?

    fun markBreak(name: String?, sourceElement: KtSourceElement): JumpDesugaringData {
        val position = findLoop(name) ?: return JumpDesugaringData.Error(ConeForEachTargetDoesNotExist)
        return markBreak(position, sourceElement)
    }

    fun markBreak(loopPosition: LoopPosition, sourceElement: KtSourceElement): JumpDesugaringData =
        markBreak(loopPosition, setOf(sourceElement))

    fun markBreak(loopPosition: LoopPosition, sources: Set<KtSourceElement>): JumpDesugaringData

    fun markContinue(name: String?, sourceElement: KtSourceElement): JumpDesugaringData {
        val position = findLoop(name) ?: return JumpDesugaringData.Error(ConeForEachTargetDoesNotExist)
        return markContinue(position, sourceElement)
    }

    fun markContinue(loopPosition: LoopPosition, sourceElement: KtSourceElement): JumpDesugaringData =
        markContinue(loopPosition, setOf(sourceElement))

    fun markContinue(loopPosition: LoopPosition, sources: Set<KtSourceElement>): JumpDesugaringData

    fun markReturn(name: String?, sourceElement: KtSourceElement, resultExpression: FirExpression? = null): JumpDesugaringData {
        val position = findFunction(name) ?: return JumpDesugaringData.Error(ConeForEachTargetDoesNotExist)
        return markReturn(position, sourceElement, resultExpression)
    }

    fun markReturn(
        functionPosition: FunctionPosition,
        sourceElement: KtSourceElement,
        resultExpression: FirExpression? = null
    ): JumpDesugaringData = markReturn(functionPosition, setOf(sourceElement), resultExpression)

    fun markReturn(
        functionPosition: FunctionPosition,
        sources: Set<KtSourceElement>,
        resultExpression: FirExpression? = null
    ): JumpDesugaringData

    // Represents the case when we haven't visited a (nested) forEach expression yet, and we're gathering the information about outer loops enclosing it
    data class Pending(override val previousScope: Completed? = null) : ForEachScope {

        private val outerLoops: MutableSet<LoopTarget.AnyLoop> = mutableSetOf()
        private val orderedOuterLoops: Deque<LoopTarget.AnyLoop> = LinkedList()

        private val outerFunctions: MutableSet<FunctionTarget> = mutableSetOf()
        private val orderedOuterFunctions: Deque<FunctionTarget> = LinkedList()

        operator fun plusAssign(loop: LoopTarget.AnyLoop) {
            require(loop !in this) { "The loop $loop is already in the scope!" }
            orderedOuterLoops.push(loop)
            outerLoops += loop
        }

        operator fun plusAssign(function: FunctionTarget) {
            require(function !in this) { "The function $function is already in the scope!" }
            orderedOuterFunctions.push(function)
            outerFunctions += function
        }

        operator fun minusAssign(loop: LoopTarget.AnyLoop) {
            require(loop == orderedOuterLoops.peek()) { "The loop $loop is not at the top of the stack!" }
            orderedOuterLoops.pop()
            outerLoops -= loop
        }

        operator fun minusAssign(function: FunctionTarget) {
            require(function == orderedOuterFunctions.peek()) { "The function $function is not at the top of the stack!" }
            orderedOuterFunctions.pop()
            outerFunctions -= function
        }

        override val previousCompletedScope: Completed? get() = previousScope

        override fun findLoop(name: String?, nextCompleted: Completed?): LoopPosition? = when {
            outerLoops.isEmpty() -> previousCompletedScope?.findLoop(name, nextCompleted)
            else -> {
                val target = when (name) {
                    null -> orderedOuterLoops.peek()
                    // Inner loops with the same label as some outer loop shadows it
                    else -> orderedOuterLoops.firstOrNull { loop -> loop.firTarget.labelName == name }
                } ?: return previousCompletedScope?.findLoop(name, nextCompleted)
                JumpablePosition(nextCompleted, target)
            }
        }

        override fun findFunction(name: String?, nextCompleted: Completed?): FunctionPosition? = when {
            outerFunctions.isEmpty() -> previousCompletedScope?.findFunction(name, nextCompleted)
            else -> {
                val target = when (name) {
                    null -> orderedOuterFunctions.firstOrNull { !it.firTarget.isLambda }
                    // "Inner" functions (lambdas) with the same label as some "outer" function shadows it
                    else -> orderedOuterFunctions.firstOrNull { function -> function.firTarget.labelName == name }
                } ?: return previousCompletedScope?.findFunction(name, nextCompleted)
                JumpablePosition(nextCompleted, target)
            }
        }

        override fun markBreak(loopPosition: LoopPosition, sources: Set<KtSourceElement>): JumpDesugaringData {
            val [scope, loop] = loopPosition
            return when (scope) {
                // Breaking from outer loops in this scope is safe, no need to desugar
                null -> when {
                    loop in this -> loop.asAnyLoop()?.let {
                        JumpDesugaringData.DefaultDesugaringOrNone { sourceElement ->
                            buildBreakExpression {
                                source = sourceElement
                                target = it.firTarget
                            }
                        }
                    } ?: JumpDesugaringData.Error(ConeForEachExpectedAnyLoop)
                    else -> JumpDesugaringData.Error(ConeForEachUnexpectedTargetInInnermostScope(loop.firTarget))
                }
                // If the target is not in this scope, it might (possibly) be in the previous one, which means there is an outer forEach loop
                // that must generate a guard, and we will have to desugar such `break` expression
                else -> previousCompletedScope?.markBreak(loopPosition, sources)
                    ?: JumpDesugaringData.Error(ConeForEachUnknownTarget(loop.firTarget))
            }
        }

        override fun markContinue(loopPosition: LoopPosition, sources: Set<KtSourceElement>): JumpDesugaringData {
            val [scope, loop] = loopPosition
            return when (scope) {
                null -> when {
                    loop in this -> loop.asAnyLoop()?.let {
                        // Continuing from outer loops in this scope is safe, again no need to desugar
                        JumpDesugaringData.DefaultDesugaringOrNone { sourceElement ->
                            buildContinueExpression {
                                source = sourceElement
                                target = it.firTarget
                            }
                        }
                    } ?: JumpDesugaringData.Error(ConeForEachExpectedAnyLoop)
                    else -> JumpDesugaringData.Error(ConeForEachUnexpectedTargetInInnermostScope(loop.firTarget))
                }
                // If the target is not in this scope, it might (possibly) be in the previous one, the case is the same as with `markBreak`
                else -> previousCompletedScope?.markContinue(loopPosition, sources)
                    ?: JumpDesugaringData.Error(ConeForEachUnknownTarget(loop.firTarget))
            }
        }

        override fun markReturn(
            functionPosition: FunctionPosition,
            sources: Set<KtSourceElement>,
            resultExpression: FirExpression?
        ): JumpDesugaringData {
            val [scope, function] = functionPosition
            return when (scope) {
                null -> when {
                    // Returning from outer functions in this scope is safe (assuming the functions are resolved correctly w.r.t. inlining),
                    // so we do not need to desugar (same with loops)
                    function in this -> JumpDesugaringData.DefaultDesugaringOrNone { sourceElement ->
                        buildReturnExpression {
                            source = sourceElement
                            target = function.firTarget
                            result = resultExpression ?: buildUnitExpression {
                                source = sourceElement.fakeElement(KtFakeSourceElementKind.ImplicitUnit.Return)
                            }
                        }
                    }
                    else -> JumpDesugaringData.Error(ConeForEachUnexpectedTargetInInnermostScope(function.firTarget))
                }
                // If the target is not in this scope, it might (possibly) be in the previous one, the case is the same as with loops
                else -> previousCompletedScope?.markReturn(functionPosition, sources, resultExpression)
                    ?: JumpDesugaringData.Error(ConeForEachUnknownTarget(function.firTarget))
            }
        }

        override val size: Int get() = outerLoops.size + outerFunctions.size

        override fun isEmpty(): Boolean = outerLoops.isEmpty() && outerFunctions.isEmpty()

        override fun contains(element: JumpableTarget<*>): Boolean = element in outerLoops || element in outerFunctions

        override fun iterator(): Iterator<JumpableTarget<*>> = sequence {
            yieldAll(outerLoops)
            yieldAll(outerFunctions)
        }.iterator()

        override fun containsAll(elements: Collection<JumpableTarget<*>>): Boolean = elements.all(::contains)
    }

    // Represents the case when we have visited a forEach expression, and after it's body visitation, we want to generate the temporary
    // variables according to the outer-loop-referencing jump expressions used in its body.
    // Additionally, `Completed` scopes are not used for discovering new outer loops as the set of outer loops for it forEach target is
    // considered (fully) collected.
    data class Completed(
        override val previousScope: Pending,
        val target: LoopTarget.ForEach,
        val sourceElement: KtSourceElement,
        val moduleData: FirModuleData
    ) : ForEachScope by previousScope {

        private fun JumpableTarget<*>.generateTemporaryVariable(prefix: String, typeRef: FirTypeRef): FirProperty {
            // If we're generating a temporary variable, it's target must definitely have a label as it is an outer loop
            val propertyName = Name.special("<$prefix-${firTarget.labelName ?: ""}>")
            return buildProperty {
                source = sourceElement.fakeElement(KtFakeSourceElementKind.DesugaredForEachTemporaryVariable(propertyName))
                moduleData = this@Completed.moduleData
                origin = FirDeclarationOrigin.Synthetic.GeneratedForEachTemporaryVariable
                returnTypeRef = typeRef
                name = propertyName
                symbol = FirLocalPropertySymbol()
                isVar = true
                status = FirResolvedDeclarationStatusImpl(Visibilities.Local, Modality.FINAL, EffectiveVisibility.Local)
                isLocal = true
            }
        }

        private fun LoopTarget.generateBreakVariable(): FirProperty =
            generateTemporaryVariable("break", moduleData.session.builtinTypes.booleanType)

        private fun LoopTarget.generateContinueVariable(): FirProperty =
            generateTemporaryVariable("continue", moduleData.session.builtinTypes.booleanType)

        private fun FunctionTarget.generateResultVariable(): FirProperty =
            generateTemporaryVariable("result", resultTypeRef)

        private fun FunctionTarget.generateReturnVariable(): FirProperty =
            generateTemporaryVariable("return", moduleData.session.builtinTypes.booleanType)

        private inline val <T : JumpableTarget<*>> T.currentPosition: JumpablePosition<T>
            get() = JumpablePosition(scope = this@Completed, target = this)

        // We need to track the information of the actual/local uses of the outer structures in order to minimize the amount of generated guards
        private val actualOuterJumpableUses: MutableMap<JumpableTarget<*>, JumpableUse<*>> = mutableMapOf()

        val outerJumpableUses: Sequence<JumpableUse<*>> get() = actualOuterJumpableUses.asSequence().map { it.value }

        override val previousCompletedScope: Completed? get() = previousScope.previousCompletedScope

        override fun findLoop(name: String?, nextCompleted: Completed?): LoopPosition? = when (name) {
            null -> target.currentPosition
            else if target.labelName == name -> target.currentPosition
            else -> previousScope.findLoop(name, this)
        }

        override fun findFunction(name: String?, nextCompleted: Completed?): FunctionPosition? =
            previousScope.findFunction(name, this)

        override fun markBreak(loopPosition: LoopPosition, sources: Set<KtSourceElement>): JumpDesugaringData =
            when (val loop = loopPosition.target) {
                // Breaking from the current `forEach` loop does not cause desugaring
                target -> JumpDesugaringData.DefaultDesugaringOrNone { sourceElement ->
                    buildReturnExpression {
                        source = sourceElement
                        // For some reason, `loop` is not smartcast into LoopTarget.ForEach
                        target = this@Completed.target.firTarget
                        result = buildLiteralExpression(source, ConstantValueKind.Boolean, false, setType = true)
                    }
                }
                // Breaking from outer loops causes desugaring in `forEach` lambdas
                else -> {
                    val prevLoopUse = actualOuterJumpableUses[loop]?.asLoopUse()
                    val breakVariable = prevLoopUse?.breakVariable ?: loop.generateBreakVariable()
                    val loopUse = prevLoopUse?.markBreak(breakVariable, sources) ?: LoopUse.BreakOnly(loopPosition, breakVariable, sources)
                    actualOuterJumpableUses[loop] = loopUse
                    JumpDesugaringData.DesugaringGivenUse(this, loopUse, JumpDesugaringKind.BreakFlag)
                }
            }

        override fun markContinue(loopPosition: LoopPosition, sources: Set<KtSourceElement>): JumpDesugaringData =
            when (val loop = loopPosition.target) {
                // Breaking from the current `forEach` loop does not cause desugaring
                target -> JumpDesugaringData.DefaultDesugaringOrNone { sourceElement ->
                    buildReturnExpression {
                        source = sourceElement
                        // For some reason, `loop` is not smartcast into LoopTarget.ForEach
                        target = this@Completed.target.firTarget
                        result = buildLiteralExpression(source, ConstantValueKind.Boolean, true, setType = true)
                    }
                }
                // Breaking from outer loops causes desugaring in `forEach` lambdas
                else -> {
                    val prevLoopUse = actualOuterJumpableUses[loop]?.asLoopUse()
                    val continueVariable = prevLoopUse?.breakVariable ?: loop.generateContinueVariable()
                    val loopUse = prevLoopUse?.markContinue(continueVariable, sources)
                        ?: LoopUse.ContinueOnly(loopPosition, continueVariable, sources)
                    actualOuterJumpableUses[loop] = loopUse
                    JumpDesugaringData.DesugaringGivenUse(this, loopUse, JumpDesugaringKind.ContinueFlag)
                }
            }

        override fun markReturn(
            functionPosition: FunctionPosition,
            sources: Set<KtSourceElement>,
            resultExpression: FirExpression?
        ): JumpDesugaringData {
            // Returning to outer functions causes desugaring in `forEach` lambdas
            val function = functionPosition.target
            val prevFunctionUse = actualOuterJumpableUses[function]?.asFunctionUse()
            val resultVariable = prevFunctionUse?.resultVariable ?: when {
                function.resultTypeRef.coneTypeOrNull == StandardTypes.Unit -> null
                else -> function.generateResultVariable()
            }
            if (resultVariable != null && function.isAnonymousFunction) function += resultVariable.symbol
            val returnVariable = prevFunctionUse?.returnVariable ?: function.generateReturnVariable()
            val functionUse = prevFunctionUse?.markReturn(resultVariable, returnVariable, sources)
                ?: FunctionUse(functionPosition, resultVariable, returnVariable, sources)
            actualOuterJumpableUses[function] = functionUse
            return JumpDesugaringData.DesugaringGivenUse(this, functionUse, JumpDesugaringKind.ResultAndReturnFlag(resultExpression))
        }
    }
}
