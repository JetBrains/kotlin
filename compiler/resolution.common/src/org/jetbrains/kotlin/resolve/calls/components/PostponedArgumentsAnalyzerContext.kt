/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.components

import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilder
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.resolve.calls.inference.model.VariableWithConstraints
import org.jetbrains.kotlin.types.model.*

interface PostponedArgumentsAnalyzerContext : TypeSystemInferenceExtensionContext {
    val notFixedTypeVariables: Map<TypeConstructorMarker, VariableWithConstraints>

    fun buildCurrentSubstitutor(additionalBindings: Map<TypeConstructorMarker, KotlinTypeMarker>): TypeSubstitutorMarker
    fun buildNotFixedVariablesToStubTypesSubstitutor(): TypeSubstitutorMarker
    fun bindingStubsForPostponedVariables(): Map<TypeVariableMarker, StubTypeMarker>

    // type can be proper if it not contains not fixed type variables
    fun canBeProper(type: KotlinTypeMarker): Boolean

    fun hasUpperOrEqualUnitConstraint(type: KotlinTypeMarker): Boolean

    fun removePostponedTypeVariablesFromConstraints(postponedTypeVariables: Set<TypeConstructorMarker>)

    // mutable operations
    fun addOtherSystem(otherSystem: ConstraintStorage)

    fun getBuilder(): ConstraintSystemBuilder
    fun resolveForkPointsConstraints()
}
