/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference

import org.jetbrains.kotlin.resolve.calls.components.PostponedArgumentsAnalyzerContext
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintSystemCompletionContext
import org.jetbrains.kotlin.resolve.calls.inference.model.Constraint
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintSystemError
import org.jetbrains.kotlin.resolve.checkers.EmptyIntersectionTypeInfo
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeVariableMarker

interface NewConstraintSystem {
    val hasContradiction: Boolean
    val errors: List<ConstraintSystemError>

    fun getBuilder(): ConstraintSystemBuilder

    // after this method we shouldn't mutate system via ConstraintSystemBuilder
    fun asReadOnlyStorage(): ConstraintStorage

    fun asConstraintSystemCompleterContext(): ConstraintSystemCompletionContext
    fun asPostponedArgumentsAnalyzerContext(): PostponedArgumentsAnalyzerContext
    fun processForkConstraints()

    fun getEmptyIntersectionTypeKind(types: Collection<KotlinTypeMarker>): EmptyIntersectionTypeInfo?
}

typealias ForkPointData = List<ConstraintsFromSingleFork>
typealias ConstraintsFromSingleFork = Set<Pair<TypeVariableMarker, Constraint>>
