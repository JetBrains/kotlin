/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirEmptyExpressionBlock
import org.jetbrains.kotlin.fir.resolve.transformers.firUnsafe
import org.jetbrains.kotlin.fir.resolve.withNullability
import org.jetbrains.kotlin.fir.returnExpressions
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilder
import org.jetbrains.kotlin.resolve.calls.inference.addSubtypeConstraintIfCompatible
import org.jetbrains.kotlin.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition
import org.jetbrains.kotlin.resolve.calls.model.PostponedResolvedAtomMarker


fun resolveArgumentExpression(
    /*
    csBuilder: ConstraintSystemBuilder,
    argument: KotlinCallArgument,
    expectedType: UnwrappedType?,
    diagnosticsHolder: KotlinDiagnosticsHolder,
    isReceiver: Boolean
     */
    csBuilder: ConstraintSystemBuilder,
    argument: FirExpression,
    expectedType: ConeKotlinType,
    expectedTypeRef: FirTypeRef,
    sink: CheckerSink,
    isReceiver: Boolean,
    isSafeCall: Boolean,
    acceptLambdaAtoms: (PostponedResolvedAtomMarker) -> Unit,
    typeProvider: (FirExpression) -> FirTypeRef?
) {
    return when (argument) {
        is FirFunctionCall, is FirCallLikeControlFlowExpression -> resolveSubCallArgument(
            csBuilder,
            argument as FirResolvable,
            expectedType,
            sink,
            isReceiver,
            isSafeCall,
            typeProvider
        )
        is FirQualifiedAccessExpression -> resolvePlainExpressionArgument(
            csBuilder,
            argument,
            expectedType,
            sink,
            isReceiver,
            isSafeCall,
            typeProvider
        )
        // TODO:!
        is FirAnonymousFunction -> preprocessLambdaArgument(csBuilder, argument, expectedType, expectedTypeRef, acceptLambdaAtoms)
        // TODO:!
        is FirCallableReferenceAccess -> Unit
        // TODO:!
        //TODO: Collection literal
        is FirWrappedArgumentExpression -> resolveArgumentExpression(
            csBuilder,
            argument.expression,
            expectedType,
            expectedTypeRef,
            sink,
            isReceiver,
            isSafeCall,
            acceptLambdaAtoms,
            typeProvider
        )
        is FirBlock -> resolveBlockArgument(csBuilder, argument, expectedType, expectedTypeRef, sink, isReceiver, isSafeCall, acceptLambdaAtoms, typeProvider)
        else -> resolvePlainExpressionArgument(csBuilder, argument, expectedType, sink, isReceiver, isSafeCall, typeProvider)
    }
}

private fun resolveBlockArgument(
    csBuilder: ConstraintSystemBuilder,
    block: FirBlock,
    expectedType: ConeKotlinType,
    expectedTypeRef: FirTypeRef,
    sink: CheckerSink,
    isReceiver: Boolean,
    isSafeCall: Boolean,
    acceptLambdaAtoms: (PostponedResolvedAtomMarker) -> Unit,
    typeProvider: (FirExpression) -> FirTypeRef?
) {
    val returnArguments = block.returnExpressions()
    if (returnArguments.isEmpty()) {
        checkApplicabilityForArgumentType(
            csBuilder,
            block.typeRef.coneTypeUnsafe(),
            expectedType.type,
            SimpleConstraintSystemConstraintPosition,
            false,
            expectedType.type.withNullability(ConeNullability.NULLABLE),
            sink
        )
        return
    }
    for (argument in returnArguments) {
        resolveArgumentExpression(csBuilder, argument, expectedType, expectedTypeRef, sink, isReceiver, isSafeCall, acceptLambdaAtoms, typeProvider)
    }
}

fun resolveSubCallArgument(
    csBuilder: ConstraintSystemBuilder,
    argument: FirResolvable,
    expectedType: ConeKotlinType,
    sink: CheckerSink,
    isReceiver: Boolean,
    isSafeCall: Boolean,
    typeProvider: (FirExpression) -> FirTypeRef?
) {
    val candidate = argument.candidate() ?: return resolvePlainExpressionArgument(csBuilder, argument as FirExpression, expectedType, sink, isReceiver, isSafeCall, typeProvider)
    val type = sink.components.returnTypeCalculator.tryCalculateReturnType(candidate.symbol.firUnsafe()).coneTypeUnsafe<ConeKotlinType>()
    val argumentType = candidate.substitutor.substituteOrSelf(type)
    resolvePlainArgumentType(csBuilder, argumentType, expectedType, sink, isReceiver, isSafeCall)
}

fun resolvePlainExpressionArgument(
    csBuilder: ConstraintSystemBuilder,
    argument: FirExpression,
    expectedType: ConeKotlinType?,
    sink: CheckerSink,
    isReceiver: Boolean,
    isSafeCall: Boolean,
    typeProvider: (FirExpression) -> FirTypeRef?
) {
    if (expectedType == null) return
    val argumentType = typeProvider(argument)?.coneTypeSafe<ConeKotlinType>() ?: return
    resolvePlainArgumentType(csBuilder, argumentType, expectedType, sink, isReceiver, isSafeCall)
}

fun resolvePlainArgumentType(
    csBuilder: ConstraintSystemBuilder,
    argumentType: ConeKotlinType,
    expectedType: ConeKotlinType,
    sink: CheckerSink,
    isReceiver: Boolean,
    isSafeCall: Boolean
) {
    val position = SimpleConstraintSystemConstraintPosition //TODO

    val nullableExpectedType = expectedType.withNullability(ConeNullability.NULLABLE)
    if (isReceiver && isSafeCall) {
        if (!csBuilder.addSubtypeConstraintIfCompatible(argumentType, nullableExpectedType, position)) {
            sink.reportApplicability(CandidateApplicability.WRONG_RECEIVER) // TODO
        }
        return
    }

    checkApplicabilityForArgumentType(csBuilder, argumentType, expectedType, position, isReceiver, nullableExpectedType, sink)
}

private fun checkApplicabilityForArgumentType(
    csBuilder: ConstraintSystemBuilder,
    argumentType: ConeKotlinType,
    expectedType: ConeKotlinType,
    position: SimpleConstraintSystemConstraintPosition,
    isReceiver: Boolean,
    nullableExpectedType: ConeKotlinType,
    sink: CheckerSink
) {
    if (!csBuilder.addSubtypeConstraintIfCompatible(argumentType, expectedType, position)) {
        if (!isReceiver) {
            if (!csBuilder.addSubtypeConstraintIfCompatible(argumentType, nullableExpectedType, position)) {
                csBuilder.addSubtypeConstraint(argumentType, expectedType, position)
            }
            return
        }
        if (csBuilder.addSubtypeConstraintIfCompatible(argumentType, nullableExpectedType, position)) {
            sink.reportApplicability(CandidateApplicability.WRONG_RECEIVER) // TODO
        } else {
            csBuilder.addSubtypeConstraint(argumentType, expectedType, position)
            sink.reportApplicability(CandidateApplicability.WRONG_RECEIVER)
        }
    }
}

internal fun Candidate.resolveArgument(
    argument: FirExpression,
    parameter: FirValueParameter,
    isReceiver: Boolean,
    isSafeCall: Boolean,
    typeProvider: (FirExpression) -> FirTypeRef?,
    sink: CheckerSink
) {

    val expectedType = prepareExpectedType(sink.components.session, argument, parameter)
    resolveArgumentExpression(
        this.system.getBuilder(),
        argument,
        expectedType,
        parameter.returnTypeRef,
        sink,
        isReceiver,
        isSafeCall,
        { this.postponedAtoms += it },
        typeProvider
    )
}

private fun Candidate.prepareExpectedType(session: FirSession, argument: FirExpression, parameter: FirValueParameter): ConeKotlinType {
    val expectedType = argument.getExpectedType(session, parameter/*, LanguageVersionSettings*/)
    return this.substitutor.substituteOrSelf(expectedType)
}

internal fun FirExpression.getExpectedType(
    session: FirSession,
    parameter: FirValueParameter/*, languageVersionSettings: LanguageVersionSettings*/
) =
//    if (this.isSpread || this.isArrayAssignedAsNamedArgumentInAnnotation(parameter, languageVersionSettings)) {
//        parameter.type.unwrap()
//    } else {
    if (parameter.isVararg && (this !is FirWrappedArgumentExpression || !isSpread)) {
        parameter.returnTypeRef.coneTypeUnsafe<ConeKotlinType>().varargElementType(session)
    } else {
        parameter.returnTypeRef.coneTypeUnsafe()
    }//?.varargElementType?.unwrap() ?: parameter.type.unwrap()
//    }


private fun ConeKotlinType.varargElementType(session: FirSession): ConeKotlinType {
    return this.arrayElementType(session) ?: error("Failed to extract! ${this.render()}!")
}