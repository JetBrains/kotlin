/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.builder.buildErrorFunction
import org.jetbrains.kotlin.fir.declarations.builder.buildErrorProperty
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.isIntegerLiteralOrOperatorCall
import org.jetbrains.kotlin.fir.returnExpressions
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.impl.originalForWrappedIntegerOperator
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.resolve.calls.components.PostponedArgumentsAnalyzerContext
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind

class CandidateFactory private constructor(
    val context: ResolutionContext,
    private val baseSystem: ConstraintStorage
) {

    companion object {
        private fun buildBaseSystem(context: ResolutionContext, callInfo: CallInfo): ConstraintStorage {
            val system = context.inferenceComponents.createConstraintSystem()
            callInfo.arguments.forEach {
                system.addSubsystemFromExpression(it)
            }
            system.addOtherSystem(context.bodyResolveContext.inferenceSession.currentConstraintStorage)
            return system.asReadOnlyStorage()
        }
    }

    constructor(context: ResolutionContext, callInfo: CallInfo) : this(context, buildBaseSystem(context, callInfo))

    fun createCandidate(
        callInfo: CallInfo,
        symbol: FirBasedSymbol<*>,
        explicitReceiverKind: ExplicitReceiverKind,
        scope: FirScope?,
        dispatchReceiverValue: ReceiverValue? = null,
        givenExtensionReceiverOptions: List<ReceiverValue> = emptyList(),
        objectsByName: Boolean = false
    ): Candidate {
        @Suppress("NAME_SHADOWING")
        val symbol = symbol.unwrapIntegerOperatorSymbolIfNeeded(callInfo)

        val result = Candidate(
            symbol, dispatchReceiverValue, givenExtensionReceiverOptions,
            explicitReceiverKind, context.inferenceComponents.constraintSystemFactory, baseSystem,
            callInfo,
            scope,
            isFromCompanionObjectTypeScope = when (explicitReceiverKind) {
                ExplicitReceiverKind.EXTENSION_RECEIVER ->
                    givenExtensionReceiverOptions.singleOrNull().isCandidateFromCompanionObjectTypeScope()
                ExplicitReceiverKind.DISPATCH_RECEIVER -> dispatchReceiverValue.isCandidateFromCompanionObjectTypeScope()
                // The following cases are not applicable for companion objects.
                ExplicitReceiverKind.NO_EXPLICIT_RECEIVER, ExplicitReceiverKind.BOTH_RECEIVERS -> false
            }
        )

        // The counterpart in FE 1.0 checks if the given descriptor is VariableDescriptor yet not PropertyDescriptor.
        // Here, we explicitly check if the referred declaration/symbol is value parameter, local variable, or backing field.
        val callSite = callInfo.callSite
        if (callSite is FirCallableReferenceAccess) {
            if (symbol is FirValueParameterSymbol || symbol is FirPropertySymbol && symbol.isLocal || symbol is FirBackingFieldSymbol) {
                result.addDiagnostic(Unsupported("References to variables aren't supported yet", callSite.calleeReference.source))
            }
        } else if (objectsByName &&
            symbol is FirRegularClassSymbol &&
            symbol.classKind != ClassKind.OBJECT &&
            symbol.companionObjectSymbol == null
        ) {
            result.addDiagnostic(NoCompanionObject)
        }
        if (callInfo.origin == FirFunctionCallOrigin.Operator && symbol is FirPropertySymbol) {
            // Flag all property references that are resolved from an convention operator call.
            result.addDiagnostic(PropertyAsOperator)
        }
        return result
    }

    private fun FirBasedSymbol<*>.unwrapIntegerOperatorSymbolIfNeeded(callInfo: CallInfo): FirBasedSymbol<*> {
        if (this !is FirNamedFunctionSymbol) return this
        // There is no need to unwrap unary operators
        if (fir.valueParameters.isEmpty()) return this
        val original = fir.originalForWrappedIntegerOperator ?: return this
        return if (callInfo.arguments.first().isIntegerLiteralOrOperatorCall()) {
            this
        } else {
            original
        }
    }

    private fun ReceiverValue?.isCandidateFromCompanionObjectTypeScope(): Boolean {
        val expressionReceiverValue = this as? ExpressionReceiverValue ?: return false
        val resolvedQualifier = (expressionReceiverValue.explicitReceiver as? FirResolvedQualifier) ?: return false
        val originClassOfCandidate = expressionReceiverValue.type.classId ?: return false
        return (resolvedQualifier.symbol?.fir as? FirRegularClass)?.companionObjectSymbol?.classId == originClassOfCandidate
    }

    fun createErrorCandidate(callInfo: CallInfo, diagnostic: ConeDiagnostic): Candidate {
        val symbol: FirBasedSymbol<*> = when (callInfo.callKind) {
            is CallKind.VariableAccess -> createErrorPropertySymbol(diagnostic)
            is CallKind.Function,
            is CallKind.DelegatingConstructorCall,
            is CallKind.CallableReference -> createErrorFunctionSymbol(diagnostic)
            is CallKind.SyntheticSelect -> throw IllegalStateException()
            is CallKind.SyntheticIdForCallableReferencesResolution -> throw IllegalStateException()
            is CallKind.CustomForIde -> throw IllegalStateException()
        }
        return Candidate(
            symbol,
            dispatchReceiverValue = null,
            givenExtensionReceiverOptions = emptyList(),
            explicitReceiverKind = ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
            context.inferenceComponents.constraintSystemFactory,
            baseSystem,
            callInfo,
            originScope = null,
        )
    }

    private fun createErrorFunctionSymbol(diagnostic: ConeDiagnostic): FirErrorFunctionSymbol {
        return FirErrorFunctionSymbol().also {
            buildErrorFunction {
                moduleData = context.session.moduleData
                resolvePhase = FirResolvePhase.BODY_RESOLVE
                origin = FirDeclarationOrigin.Synthetic
                this.diagnostic = diagnostic
                symbol = it
            }
        }
    }

    private fun createErrorPropertySymbol(diagnostic: ConeDiagnostic): FirErrorPropertySymbol {
        return FirErrorPropertySymbol(diagnostic).also {
            buildErrorProperty {
                moduleData = context.session.moduleData
                resolvePhase = FirResolvePhase.BODY_RESOLVE
                origin = FirDeclarationOrigin.Synthetic
                name = FirErrorPropertySymbol.NAME
                this.diagnostic = diagnostic
                symbol = it
            }
        }
    }
}

fun PostponedArgumentsAnalyzerContext.addSubsystemFromExpression(statement: FirStatement) {
    when (statement) {
        is FirQualifiedAccessExpression,
        is FirWhenExpression,
        is FirTryExpression,
        is FirCheckNotNullCall,
        is FirElvisExpression
        -> (statement as FirResolvable).candidate()?.let { addOtherSystem(it.system.asReadOnlyStorage()) }

        is FirSafeCallExpression -> addSubsystemFromExpression(statement.selector)
        is FirWrappedArgumentExpression -> addSubsystemFromExpression(statement.expression)
        is FirBlock -> statement.returnExpressions().forEach { addSubsystemFromExpression(it) }
    }
}

internal fun FirResolvable.candidate(): Candidate? {
    return when (val callee = this.calleeReference) {
        is FirNamedReferenceWithCandidate -> return callee.candidate
        else -> null
    }
}
