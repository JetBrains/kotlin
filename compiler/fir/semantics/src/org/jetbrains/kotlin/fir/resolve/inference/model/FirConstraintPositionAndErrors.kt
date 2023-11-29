/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference.model

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.types.ConeTypeVariable
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.resolve.calls.inference.model.*
import org.jetbrains.kotlin.types.model.TypeVariableMarker

class ConeDeclaredUpperBoundConstraintPosition : DeclaredUpperBoundConstraintPosition<Nothing?>(null) {
    override fun toString(): String = "DeclaredUpperBound"
}

class ConeFixVariableConstraintPosition(variable: TypeVariableMarker) : FixVariableConstraintPosition<Nothing?>(variable, null) {
    override fun toString(): String = "Fix variable ${(variable as ConeTypeVariable).typeConstructor.name}"
}

class ConeArgumentConstraintPosition(argument: FirElement) : ArgumentConstraintPosition<FirElement>(argument) {
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

class ConeLambdaArgumentConstraintPosition(
    anonymousFunction: FirAnonymousFunction
) : LambdaArgumentConstraintPosition<FirAnonymousFunction>(anonymousFunction) {
    override fun toString(): String {
        return "LambdaArgument"
    }
}


class ConeBuilderInferenceSubstitutionConstraintPosition(initialConstraint: InitialConstraint) :
    BuilderInferenceSubstitutionConstraintPosition<Nothing?>(null, initialConstraint) {
    override fun toString(): String = "Incorporated builder inference constraint $initialConstraint " +
            "into some call"
}

class ConeReceiverConstraintPosition(receiver: FirExpression) : ReceiverConstraintPosition<FirExpression>(receiver) {
    override fun toString(): String = "Receiver ${argument.render()}"

}
