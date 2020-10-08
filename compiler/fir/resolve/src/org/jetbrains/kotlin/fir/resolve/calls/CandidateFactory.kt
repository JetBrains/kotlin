/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.builder.buildErrorFunction
import org.jetbrains.kotlin.fir.declarations.builder.buildErrorProperty
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.returnExpressions
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirErrorFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirErrorPropertySymbol
import org.jetbrains.kotlin.resolve.calls.components.PostponedArgumentsAnalyzerContext
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind

class CandidateFactory private constructor(
    val context: ResolutionContext,
    val callInfo: CallInfo,
    private val baseSystem: ConstraintStorage
) {

    companion object {
        private fun buildBaseSystem(context: ResolutionContext, callInfo: CallInfo): ConstraintStorage {
            val system = context.inferenceComponents.createConstraintSystem()
            callInfo.arguments.forEach {
                system.addSubsystemFromExpression(it)
            }
            system.addOtherSystem(context.bodyResolveContext.inferenceSession.currentConstraintSystem)
            return system.asReadOnlyStorage()
        }
    }

    constructor(context: ResolutionContext, callInfo: CallInfo) :
            this(context, callInfo, buildBaseSystem(context, callInfo))

    fun replaceCallInfo(callInfo: CallInfo): CandidateFactory {
        if (this.callInfo.arguments.size != callInfo.arguments.size) {
            throw AssertionError("Incorrect replacement of call info in CandidateFactory")
        }
        return CandidateFactory(context, callInfo, baseSystem)
    }

    fun createCandidate(
        symbol: AbstractFirBasedSymbol<*>,
        explicitReceiverKind: ExplicitReceiverKind,
        scope: FirScope?,
        dispatchReceiverValue: ReceiverValue? = null,
        implicitExtensionReceiverValue: ImplicitReceiverValue<*>? = null,
        builtInExtensionFunctionReceiverValue: ReceiverValue? = null
    ): Candidate {
        return Candidate(
            symbol, dispatchReceiverValue, implicitExtensionReceiverValue,
            explicitReceiverKind, context.inferenceComponents.constraintSystemFactory, baseSystem,
            builtInExtensionFunctionReceiverValue?.receiverExpression?.let {
                callInfo.withReceiverAsArgument(it)
            } ?: callInfo,
            scope,
        )
    }

    fun createErrorCandidate(diagnostic: ConeDiagnostic): Candidate {
        val symbol: AbstractFirBasedSymbol<*> = when (callInfo.callKind) {
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
            implicitExtensionReceiverValue = null,
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
                session = context.session
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
                session = context.session
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
        is FirFunctionCall,
        is FirQualifiedAccessExpression,
        is FirWhenExpression,
        is FirTryExpression,
        is FirCheckNotNullCall,
        is FirCallableReferenceAccess,
        is FirElvisExpression
        -> (statement as FirResolvable).candidate()?.let { addOtherSystem(it.system.asReadOnlyStorage()) }

        is FirSafeCallExpression -> addSubsystemFromExpression(statement.regularQualifiedAccess)
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
