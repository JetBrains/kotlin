/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.resolve.withNullability
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilder
import org.jetbrains.kotlin.resolve.calls.inference.addSubtypeConstraintIfCompatible
import org.jetbrains.kotlin.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition


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
    sink: CheckerSink,
    isReceiver: Boolean,
    typeProvider: (FirExpression) -> FirTypeRef?
) {
    return when (argument) {
        is FirQualifiedAccessExpression, is FirFunctionCall -> checkPlainExpressionArgument(csBuilder, argument, expectedType, sink, isReceiver, typeProvider)
        // TODO:!
        is FirAnonymousFunction -> Unit
        // TODO:!
        is FirCallableReferenceAccess -> Unit
        // TODO:!
        //TODO: Collection literal
        else -> checkPlainExpressionArgument(csBuilder, argument, expectedType, sink, isReceiver, typeProvider)
    }
}

fun checkPlainExpressionArgument(
    csBuilder: ConstraintSystemBuilder,
    argument: FirExpression,
    expectedType: ConeKotlinType?,
    sink: CheckerSink,
    isReceiver: Boolean,
    typeProvider: (FirExpression) -> FirTypeRef?
) {
    if (expectedType == null) return
    val argumentType = typeProvider(argument)?.coneTypeSafe<ConeKotlinType>() ?: return

    val position = SimpleConstraintSystemConstraintPosition //TODO

    if (!csBuilder.addSubtypeConstraintIfCompatible(argumentType, expectedType, position)) {
        if (!isReceiver) {
            csBuilder.addSubtypeConstraint(argumentType, expectedType, position)
        }
        val nullableExpectedType = expectedType.withNullability(ConeNullability.NULLABLE)
        if (csBuilder.addSubtypeConstraintIfCompatible(argumentType, nullableExpectedType, position)) {
            sink.reportApplicability(CandidateApplicability.WRONG_RECEIVER) // TODO
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
    resolveArgumentExpression(this.system.getBuilder(), argument, expectedType, sink, isReceiver, typeProvider)
}

private fun Candidate.prepareExpectedType(argument: FirExpression, parameter: FirValueParameter): ConeKotlinType {
    return argument.getExpectedType(parameter/*, LanguageVersionSettings*/)
}

internal fun FirExpression.getExpectedType(parameter: FirValueParameter/*, languageVersionSettings: LanguageVersionSettings*/) =
//    if (this.isSpread || this.isArrayAssignedAsNamedArgumentInAnnotation(parameter, languageVersionSettings)) {
//        parameter.type.unwrap()
//    } else {
    parameter.returnTypeRef.coneTypeUnsafe()//?.varargElementType?.unwrap() ?: parameter.type.unwrap()
//    }