/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference.model

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.resolve.calls.inference.model.*
import org.jetbrains.kotlin.types.model.TypeVariableMarker

class ConeDeclaredUpperBoundConstraintPosition : DeclaredUpperBoundConstraintPosition<Nothing?>(null)

class ConeFixVariableConstraintPosition(variable: TypeVariableMarker) : FixVariableConstraintPosition<Nothing?>(variable, null)

class ConeArgumentConstraintPosition(argument: FirElement) : ArgumentConstraintPosition<FirElement>(argument)

object ConeExpectedTypeConstraintPosition : ExpectedTypeConstraintPosition<Nothing?>(null)

class ConeExplicitTypeParameterConstraintPosition(
    typeArgument: FirTypeProjection,
) : ExplicitTypeParameterConstraintPosition<FirTypeProjection>(typeArgument)

class ConeLambdaArgumentConstraintPosition(
    anonymousFunction: FirAnonymousFunction
) : LambdaArgumentConstraintPosition<FirAnonymousFunction>(anonymousFunction)


class ConeBuilderInferenceSubstitutionConstraintPosition(initialConstraint: InitialConstraint) :
    BuilderInferenceSubstitutionConstraintPosition<Nothing?>(null, initialConstraint) // TODO

class ConeReceiverConstraintPosition(receiver: FirExpression) : ReceiverConstraintPosition<FirExpression>(receiver)
