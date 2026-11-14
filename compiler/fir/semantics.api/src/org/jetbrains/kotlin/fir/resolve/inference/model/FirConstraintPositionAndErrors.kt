/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference.model

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.asCone
import org.jetbrains.kotlin.resolve.calls.inference.model.*
import org.jetbrains.kotlin.types.model.TypeVariableMarker

class ConeDeclaredUpperBoundConstraintPosition : DeclaredUpperBoundConstraintPosition<Nothing?>(null) {
    override fun toString(): String = "DeclaredUpperBound"
}

class ConeSemiFixVariableConstraintPosition(variable: TypeVariableMarker) : SemiFixVariableConstraintPosition(variable) {
    override fun toString(): String = "Fix variable ${variable.asCone().typeConstructor.name}"
}

class ConeFixVariableConstraintPosition(variable: TypeVariableMarker) : FixVariableConstraintPosition<Nothing?>(variable, null) {
    override fun toString(): String = "Fix variable ${variable.asCone().typeConstructor.name}"
}

class ConeArgumentConstraintPosition(argument: FirExpression) : RegularArgumentConstraintPosition<FirExpression>(argument) {
    override fun toString(): String {
        return "Argument ${argument.render()}"
    }
}

object ConeExpectedTypeConstraintPosition : ExpectedTypeConstraintPosition<Nothing?>(null) {
    override fun toString(): String = "ExpectedType for some call"
}

class ConeExplicitTypeParameterConstraintPosition(
    typeArgument: FirTypeProjection,
) : ExplicitTypeParameterConstraintPosition<FirTypeProjection>(typeArgument) {
    override fun toString(): String = "TypeParameter ${typeArgument.render()}"
}

sealed class ConeLambdaArgumentConstraintPosition(val lambdaExpression: FirAnonymousFunctionExpression) :
    LambdaArgumentConstraintPosition<FirAnonymousFunction>(lambdaExpression.anonymousFunction) {
    abstract val anonymousFunctionReturnExpression: FirExpression?

    override fun toString(): String = "LambdaArgument"
}

class ConeRegularLambdaArgumentConstraintPosition(
    lambdaExpression: FirAnonymousFunctionExpression,
    override val anonymousFunctionReturnExpression: FirExpression,
) : ConeLambdaArgumentConstraintPosition(lambdaExpression), OnlyInputTypeConstraintPosition

// TODO: This class is different from `ConeRegularLambdaArgumentConstraintPosition` to not inherit it from `OnlyInputTypesConstraintPosition`
//  marker. Most probably, it actually should be inherited as well (see KT-80079). If so, these classes should be merged.
class ConeLambdaArgumentConstraintPositionWithCoercionToUnit(
    lambdaExpression: FirAnonymousFunctionExpression,
    override val anonymousFunctionReturnExpression: FirExpression?,
) : ConeLambdaArgumentConstraintPosition(lambdaExpression)

class ConeReceiverConstraintPosition(
    receiver: FirExpression,
    val source: KtSourceElement?,
) : ReceiverConstraintPosition<FirExpression>(receiver) {
    override fun toString(): String = "Receiver ${argument.render()}"
}
