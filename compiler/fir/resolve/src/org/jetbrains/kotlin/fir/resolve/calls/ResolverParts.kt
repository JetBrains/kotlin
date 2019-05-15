/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.resolve.FirProvider
import org.jetbrains.kotlin.fir.resolve.transformers.firSafeNullable
import org.jetbrains.kotlin.fir.resolve.transformers.firUnsafe
import org.jetbrains.kotlin.fir.service
import org.jetbrains.kotlin.fir.symbols.ConeCallableSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.ConeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.load.java.JavaVisibilities
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind.*
import java.lang.IllegalStateException


abstract class ResolutionStage {
    abstract fun check(candidate: Candidate, sink: CheckerSink, callInfo: CallInfo)
}

abstract class CheckerStage : ResolutionStage()

internal object CheckExplicitReceiverConsistency : ResolutionStage() {
    override fun check(candidate: Candidate, sink: CheckerSink, callInfo: CallInfo) {
        val receiverKind = candidate.explicitReceiverKind
        val explicitReceiver = callInfo.explicitReceiver
        // TODO: add invoke cases
        when (receiverKind) {
            NO_EXPLICIT_RECEIVER -> {
                if (explicitReceiver != null) return sink.reportApplicability(CandidateApplicability.WRONG_RECEIVER)
            }
            EXTENSION_RECEIVER, DISPATCH_RECEIVER -> {
                if (explicitReceiver == null) return sink.reportApplicability(CandidateApplicability.WRONG_RECEIVER)
            }
            BOTH_RECEIVERS -> {
                if (explicitReceiver == null) return sink.reportApplicability(CandidateApplicability.WRONG_RECEIVER)
                // Here we should also check additional invoke receiver
            }
        }
    }

}

internal sealed class CheckReceivers : ResolutionStage() {
    object Dispatch : CheckReceivers() {
        override fun ExplicitReceiverKind.shouldBeResolvedAsImplicit(): Boolean {
            return this == EXTENSION_RECEIVER || this == NO_EXPLICIT_RECEIVER
        }

        override fun ExplicitReceiverKind.shouldBeResolvedAsExplicit(): Boolean {
            return this == DISPATCH_RECEIVER || this == BOTH_RECEIVERS
        }

        override fun Candidate.getReceiverValue(): ReceiverValue? {
            return dispatchReceiverValue
        }
    }

    object Extension : CheckReceivers() {
        override fun ExplicitReceiverKind.shouldBeResolvedAsImplicit(): Boolean {
            return this == DISPATCH_RECEIVER || this == NO_EXPLICIT_RECEIVER
        }

        override fun ExplicitReceiverKind.shouldBeResolvedAsExplicit(): Boolean {
            return this == EXTENSION_RECEIVER || this == BOTH_RECEIVERS
        }

        override fun Candidate.getReceiverValue(): ReceiverValue? {
            val callableSymbol = symbol as? FirCallableSymbol ?: return null
            val callable = callableSymbol.fir
            val type = (callable.receiverTypeRef as FirResolvedTypeRef?)?.type ?: return null
            return object : ReceiverValue {
                override val type: ConeKotlinType
                    get() = type

            }
        }
    }

    abstract fun Candidate.getReceiverValue(): ReceiverValue?

    abstract fun ExplicitReceiverKind.shouldBeResolvedAsExplicit(): Boolean

    abstract fun ExplicitReceiverKind.shouldBeResolvedAsImplicit(): Boolean

    override fun check(candidate: Candidate, sink: CheckerSink, callInfo: CallInfo) {
        val receiverParameterValue = candidate.getReceiverValue()
        val explicitReceiverExpression = callInfo.explicitReceiver
        val explicitReceiverKind = candidate.explicitReceiverKind

        if (receiverParameterValue != null) {
            if (explicitReceiverExpression != null && explicitReceiverKind.shouldBeResolvedAsExplicit()) {
                resolveArgumentExpression(
                    candidate.csBuilder,
                    explicitReceiverExpression,
                    candidate.substitutor.substituteOrSelf(receiverParameterValue.type),
                    explicitReceiverExpression.typeRef,
                    sink,
                    isReceiver = true,
                    isSafeCall = callInfo.isSafeCall,
                    typeProvider = callInfo.typeProvider,
                    acceptLambdaAtoms = { candidate.postponedAtoms += it }
                )
            }
        }
    }
}

internal object MapArguments : ResolutionStage() {
    override fun check(candidate: Candidate, sink: CheckerSink, callInfo: CallInfo) {
        val symbol = candidate.symbol as? FirFunctionSymbol ?: return sink.reportApplicability(CandidateApplicability.HIDDEN)
        val function = symbol.firUnsafe<FirFunction>()
        val processor = FirCallArgumentsProcessor(function, callInfo.arguments)
        val mappingResult = processor.process()
        candidate.argumentMapping = mappingResult.argumentMapping
        if (!mappingResult.isSuccess) {
            return sink.reportApplicability(CandidateApplicability.PARAMETER_MAPPING_ERROR)
        }
    }

}

internal object CheckArguments : CheckerStage() {
    override fun check(candidate: Candidate, sink: CheckerSink, callInfo: CallInfo) {
        val argumentMapping =
            candidate.argumentMapping ?: throw IllegalStateException("Argument should be already mapped while checking arguments!")
        for ((argument, parameter) in argumentMapping) {
            candidate.resolveArgument(
                argument,
                parameter,
                isReceiver = false,
                isSafeCall = false,
                typeProvider = callInfo.typeProvider,
                sink = sink
            )
        }

        if (candidate.system.hasContradiction) {
            sink.reportApplicability(CandidateApplicability.INAPPLICABLE)
        }
    }
}

internal object DiscriminateSynthetics : CheckerStage() {
    override fun check(candidate: Candidate, sink: CheckerSink, callInfo: CallInfo) {
        if (candidate.symbol is SyntheticSymbol) {
            sink.reportApplicability(CandidateApplicability.SYNTHETIC_RESOLVED)
        }
    }

}

internal object CheckVisibility : CheckerStage() {

    private fun ConeSymbol.packageFqName(): FqName {
        return when (this) {
            is ConeClassLikeSymbol -> classId.packageFqName
            is ConeCallableSymbol -> callableId.packageName
            else -> error("No package fq name for ${this}")
        }
    }

    override fun check(candidate: Candidate, sink: CheckerSink, callInfo: CallInfo) {
        val symbol = candidate.symbol
        val declaration = symbol.firSafeNullable<FirMemberDeclaration>()
        if (declaration != null && !declaration.visibility.isPublicAPI) {
            val visible = when (declaration.visibility) {
                JavaVisibilities.PACKAGE_VISIBILITY ->
                    symbol.packageFqName() == callInfo.containingFile.packageFqName
                Visibilities.PRIVATE, Visibilities.PRIVATE_TO_THIS -> {
                    if (declaration.session == callInfo.session) {
                        val provider = callInfo.session.service<FirProvider>()
                        val candidateFile = when (symbol) {
                            is ConeCallableSymbol -> provider.getFirCallableContainerFile(symbol)
                            is ConeClassLikeSymbol -> provider.getFirClassifierContainerFile(symbol.classId)
                            else -> null
                        }
                        candidateFile == callInfo.containingFile
                    } else {
                        false
                    }
                }
                Visibilities.INTERNAL ->
                    declaration.session == callInfo.session
                else -> true
            }

            if (!visible) {
                sink.reportApplicability(CandidateApplicability.HIDDEN)
            }
        }
    }
}


internal fun functionCallResolutionSequence() = listOf(
    CheckVisibility, MapArguments, CheckExplicitReceiverConsistency, CreateFreshTypeVariableSubstitutorStage,
    CheckReceivers.Dispatch, CheckReceivers.Extension, CheckArguments
)


internal fun qualifiedAccessResolutionSequence() = listOf(
    CheckVisibility,
    DiscriminateSynthetics,
    CheckExplicitReceiverConsistency,
    CreateFreshTypeVariableSubstitutorStage,
    CheckReceivers.Dispatch,
    CheckReceivers.Extension
)