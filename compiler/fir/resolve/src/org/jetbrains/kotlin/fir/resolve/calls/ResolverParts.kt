/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.resolve.FirProvider
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


abstract class ResolutionStage {
    abstract suspend fun check(candidate: Candidate, sink: CheckerSink, callInfo: CallInfo)
}

abstract class CheckerStage : ResolutionStage()

internal object CheckExplicitReceiverConsistency : ResolutionStage() {
    override suspend fun check(candidate: Candidate, sink: CheckerSink, callInfo: CallInfo) {
        val receiverKind = candidate.explicitReceiverKind
        val explicitReceiver = callInfo.explicitReceiver
        // TODO: add invoke cases
        when (receiverKind) {
            NO_EXPLICIT_RECEIVER -> {
                if (explicitReceiver != null && explicitReceiver !is FirResolvedQualifier)
                    return sink.yieldApplicability(CandidateApplicability.WRONG_RECEIVER)
            }
            EXTENSION_RECEIVER, DISPATCH_RECEIVER -> {
                if (explicitReceiver == null) return sink.yieldApplicability(CandidateApplicability.WRONG_RECEIVER)
            }
            BOTH_RECEIVERS -> {
                if (explicitReceiver == null) return sink.yieldApplicability(CandidateApplicability.WRONG_RECEIVER)
                // Here we should also check additional invoke receiver
            }
        }
    }
}

internal sealed class CheckReceivers : ResolutionStage() {
    object Dispatch : CheckReceivers() {
        override fun ExplicitReceiverKind.shouldBeCheckedAgainstImplicit(): Boolean {
            return this == EXTENSION_RECEIVER // For NO_EXPLICIT_RECEIVER we can check extension receiver only
        }

        override fun ExplicitReceiverKind.shouldBeCheckedAgainstExplicit(): Boolean {
            return this == DISPATCH_RECEIVER || this == BOTH_RECEIVERS
        }

        override fun Candidate.getReceiverType(): ConeKotlinType? {
            return dispatchReceiverValue?.type
        }
    }

    object Extension : CheckReceivers() {
        override fun ExplicitReceiverKind.shouldBeCheckedAgainstImplicit(): Boolean {
            return this == DISPATCH_RECEIVER || this == NO_EXPLICIT_RECEIVER
        }

        override fun ExplicitReceiverKind.shouldBeCheckedAgainstExplicit(): Boolean {
            return this == EXTENSION_RECEIVER || this == BOTH_RECEIVERS
        }

        override fun Candidate.getReceiverType(): ConeKotlinType? {
            val callableSymbol = symbol as? FirCallableSymbol<*> ?: return null
            val callable = callableSymbol.fir
            return (callable.receiverTypeRef as FirResolvedTypeRef?)?.type
        }
    }

    abstract fun Candidate.getReceiverType(): ConeKotlinType?

    abstract fun ExplicitReceiverKind.shouldBeCheckedAgainstExplicit(): Boolean

    abstract fun ExplicitReceiverKind.shouldBeCheckedAgainstImplicit(): Boolean

    override suspend fun check(candidate: Candidate, sink: CheckerSink, callInfo: CallInfo) {
        val expectedReceiverType = candidate.getReceiverType()
        val explicitReceiverExpression = callInfo.explicitReceiver
        val explicitReceiverKind = candidate.explicitReceiverKind

        if (expectedReceiverType != null) {
            if (explicitReceiverExpression != null && explicitReceiverKind.shouldBeCheckedAgainstExplicit()) {
                resolveArgumentExpression(
                    candidate.csBuilder,
                    argument = explicitReceiverExpression,
                    expectedType = candidate.substitutor.substituteOrSelf(expectedReceiverType),
                    expectedTypeRef = explicitReceiverExpression.typeRef,
                    sink = sink,
                    isReceiver = true,
                    isSafeCall = callInfo.isSafeCall,
                    typeProvider = callInfo.typeProvider,
                    acceptLambdaAtoms = { candidate.postponedAtoms += it }
                )
                sink.yield()
            } else {
                val argumentExtensionReceiverValue = candidate.implicitExtensionReceiverValue
                if (argumentExtensionReceiverValue != null && explicitReceiverKind.shouldBeCheckedAgainstImplicit()) {
                    resolvePlainArgumentType(
                        candidate.csBuilder,
                        argumentType = argumentExtensionReceiverValue.type,
                        expectedType = candidate.substitutor.substituteOrSelf(expectedReceiverType.type),
                        sink = sink,
                        isReceiver = true,
                        isSafeCall = callInfo.isSafeCall
                    )
                    sink.yield()
                }
            }
        }
    }
}

internal object MapArguments : ResolutionStage() {
    override suspend fun check(candidate: Candidate, sink: CheckerSink, callInfo: CallInfo) {
        val symbol = candidate.symbol as? FirFunctionSymbol<*> ?: return sink.reportApplicability(CandidateApplicability.HIDDEN)
        val function = symbol.fir
        val processor = FirCallArgumentsProcessor(function, callInfo.arguments)
        val mappingResult = processor.process()
        candidate.argumentMapping = mappingResult.argumentMapping
        if (!mappingResult.isSuccess) {
            return sink.yieldApplicability(CandidateApplicability.PARAMETER_MAPPING_ERROR)
        }
    }
}

internal object CheckArguments : CheckerStage() {
    override suspend fun check(candidate: Candidate, sink: CheckerSink, callInfo: CallInfo) {
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
            if (candidate.system.hasContradiction) {
                sink.yieldApplicability(CandidateApplicability.INAPPLICABLE)
            }
            sink.yield()
        }
    }
}

internal object DiscriminateSynthetics : CheckerStage() {
    override suspend fun check(candidate: Candidate, sink: CheckerSink, callInfo: CallInfo) {
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

    override suspend fun check(candidate: Candidate, sink: CheckerSink, callInfo: CallInfo) {
        val symbol = candidate.symbol
        val declaration = symbol.fir
        if (declaration is FirMemberDeclaration && !declaration.visibility.isPublicAPI) {
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
                sink.yieldApplicability(CandidateApplicability.HIDDEN)
            }
        }
    }
}