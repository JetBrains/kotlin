/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.withNullability
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
    acceptLambdaAtoms: (PostponedResolvedAtomMarker) -> Unit,
    typeProvider: (FirExpression) -> FirTypeRef?
) {
    return when (argument) {
        is FirQualifiedAccessExpression, is FirFunctionCall -> resolvePlainExpressionArgument(
            csBuilder,
            argument,
            expectedType,
            sink,
            isReceiver,
            typeProvider
        )
        // TODO:!
        is FirAnonymousFunction -> preprocessLambdaArgument(csBuilder, argument, expectedType, expectedTypeRef, acceptLambdaAtoms)
        // TODO:!
        is FirCallableReferenceAccess -> Unit
        // TODO:!
        //TODO: Collection literal
        is FirLambdaArgumentExpression -> resolveArgumentExpression(
            csBuilder,
            argument.expression,
            expectedType,
            expectedTypeRef,
            sink,
            isReceiver,
            acceptLambdaAtoms,
            typeProvider
        )
        is FirNamedArgumentExpression -> resolveArgumentExpression(
            csBuilder,
            argument.expression,
            expectedType,
            expectedTypeRef,
            sink,
            isReceiver,
            acceptLambdaAtoms,
            typeProvider
        )
        else -> resolvePlainExpressionArgument(csBuilder, argument, expectedType, sink, isReceiver, typeProvider)
    }
}

fun resolvePlainExpressionArgument(
    csBuilder: ConstraintSystemBuilder,
    argument: FirExpression,
    expectedType: ConeKotlinType?,
    sink: CheckerSink,
    isReceiver: Boolean,
    typeProvider: (FirExpression) -> FirTypeRef?
) {
    if (expectedType == null) return
    val argumentType = typeProvider(argument)?.coneTypeSafe<ConeKotlinType>() ?: return
    resolvePlainArgumentType(csBuilder, argumentType, expectedType, sink, isReceiver)
}

fun resolvePlainArgumentType(
    csBuilder: ConstraintSystemBuilder,
    argumentType: ConeKotlinType,
    expectedType: ConeKotlinType,
    sink: CheckerSink,
    isReceiver: Boolean
) {
    val position = SimpleConstraintSystemConstraintPosition //TODO

    if (!csBuilder.addSubtypeConstraintIfCompatible(argumentType, expectedType, position)) {
        if (!isReceiver) {
            csBuilder.addSubtypeConstraint(argumentType, expectedType, position)
            return
        }
        val nullableExpectedType = expectedType.withNullability(ConeNullability.NULLABLE)
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
    typeProvider: (FirExpression) -> FirTypeRef?,
    sink: CheckerSink
) {

    val expectedType = prepareExpectedType(argument, parameter)
    resolveArgumentExpression(
        this.system.getBuilder(),
        argument,
        expectedType,
        parameter.returnTypeRef,
        sink,
        isReceiver,
        { this.postponedAtoms += it },
        typeProvider
    )
}

private fun Candidate.prepareExpectedType(argument: FirExpression, parameter: FirValueParameter): ConeKotlinType {
    val expectedType = argument.getExpectedType(parameter/*, LanguageVersionSettings*/)
    return this.substitutor.substituteOrSelf(expectedType)
}

internal fun FirExpression.getExpectedType(parameter: FirValueParameter/*, languageVersionSettings: LanguageVersionSettings*/) =
//    if (this.isSpread || this.isArrayAssignedAsNamedArgumentInAnnotation(parameter, languageVersionSettings)) {
//        parameter.type.unwrap()
//    } else {
    parameter.returnTypeRef.coneTypeUnsafe()//?.varargElementType?.unwrap() ?: parameter.type.unwrap()
//    }