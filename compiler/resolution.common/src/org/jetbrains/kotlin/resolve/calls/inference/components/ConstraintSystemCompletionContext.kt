/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference.components

import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilder
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintSystemError
import org.jetbrains.kotlin.resolve.calls.inference.model.FixVariableConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.model.VariableWithConstraints
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.model.TypeVariableMarker

interface ConstraintSystemCompletionContext : VariableFixationFinder.Context, ResultTypeResolver.Context {
    val allTypeVariables: Map<TypeConstructorMarker, TypeVariableMarker>
    override val notFixedTypeVariables: Map<TypeConstructorMarker, VariableWithConstraints>
    override val fixedTypeVariables: Map<TypeConstructorMarker, KotlinTypeMarker>
    override val postponedTypeVariables: List<TypeVariableMarker>

    fun getBuilder(): ConstraintSystemBuilder

    // type can be proper if it not contains not fixed type variables
    fun canBeProper(type: KotlinTypeMarker): Boolean

    fun containsOnlyFixedOrPostponedVariables(type: KotlinTypeMarker): Boolean

    // mutable operations
    fun addError(error: ConstraintSystemError)

    fun fixVariable(variable: TypeVariableMarker, resultType: KotlinTypeMarker, position: FixVariableConstraintPosition<*>)

    fun asConstraintSystemCompletionContext(): ConstraintSystemCompletionContext
}
