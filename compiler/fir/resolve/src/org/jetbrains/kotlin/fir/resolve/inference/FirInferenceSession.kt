/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference

import org.jetbrains.kotlin.fir.expressions.FirResolvable
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.resolve.calls.Candidate
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeStubType
import org.jetbrains.kotlin.fir.types.ConeTypeVariableTypeConstructor
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilder
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintSystemCompletionContext
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintSystemCompletionMode
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.resolve.calls.inference.model.NewConstraintSystemImpl
import org.jetbrains.kotlin.types.model.StubTypeMarker
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.model.TypeVariableMarker

abstract class FirInferenceSession {
    companion object {
        val DEFAULT: FirInferenceSession = object : FirStubInferenceSession() {}
    }

    abstract fun <T> shouldRunCompletion(call: T): Boolean where T : FirResolvable, T : FirStatement
    abstract val currentConstraintStorage: ConstraintStorage

    abstract fun <T> addPartiallyResolvedCall(call: T) where T : FirResolvable, T : FirStatement
    abstract fun <T> addCompletedCall(call: T, candidate: Candidate) where T : FirResolvable, T : FirStatement

    abstract fun registerStubTypes(map: Map<TypeVariableMarker, StubTypeMarker>)

    abstract fun hasSyntheticTypeVariables(): Boolean
    abstract fun isSyntheticTypeVariable(typeVariable: TypeVariableMarker): Boolean
    abstract fun fixSyntheticTypeVariableWithNotEnoughInformation(
        typeVariable: TypeVariableMarker,
        completionContext: ConstraintSystemCompletionContext
    )

    abstract fun inferPostponedVariables(
        lambda: ResolvedLambdaAtom,
        constraintSystemBuilder: ConstraintSystemBuilder,
        completionMode: ConstraintSystemCompletionMode,
        // TODO: diagnostic holder
    ): Map<ConeTypeVariableTypeConstructor, ConeKotlinType>?

    abstract fun clear()
    abstract fun createSyntheticStubTypes(system: NewConstraintSystemImpl): Map<TypeConstructorMarker, ConeStubType>
}

abstract class FirStubInferenceSession : FirInferenceSession() {
    override fun <T> shouldRunCompletion(call: T): Boolean where T : FirResolvable, T : FirStatement = true

    override val currentConstraintStorage: ConstraintStorage
        get() = ConstraintStorage.Empty

    override fun <T> addPartiallyResolvedCall(call: T) where T : FirResolvable, T : FirStatement {}
    override fun <T> addCompletedCall(call: T, candidate: Candidate) where T : FirResolvable, T : FirStatement {}

    override fun inferPostponedVariables(
        lambda: ResolvedLambdaAtom,
        constraintSystemBuilder: ConstraintSystemBuilder,
        completionMode: ConstraintSystemCompletionMode
    ): Map<ConeTypeVariableTypeConstructor, ConeKotlinType>? = null

    override fun registerStubTypes(map: Map<TypeVariableMarker, StubTypeMarker>) {}

    override fun hasSyntheticTypeVariables(): Boolean = false
    override fun isSyntheticTypeVariable(typeVariable: TypeVariableMarker): Boolean = false
    override fun fixSyntheticTypeVariableWithNotEnoughInformation(
        typeVariable: TypeVariableMarker,
        completionContext: ConstraintSystemCompletionContext
    ) {}

    override fun createSyntheticStubTypes(system: NewConstraintSystemImpl): Map<TypeConstructorMarker, ConeStubType> = emptyMap()

    override fun clear() {
    }
}
