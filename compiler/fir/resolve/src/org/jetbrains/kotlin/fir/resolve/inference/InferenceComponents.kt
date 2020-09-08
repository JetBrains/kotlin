/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.types.ConeInferenceContext
import org.jetbrains.kotlin.fir.types.ConeTypeCheckerContext
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintIncorporator
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintInjector
import org.jetbrains.kotlin.resolve.calls.inference.components.ResultTypeResolver
import org.jetbrains.kotlin.resolve.calls.inference.components.TrivialConstraintTypeInferenceOracle
import org.jetbrains.kotlin.resolve.calls.inference.model.NewConstraintSystemImpl
import org.jetbrains.kotlin.types.AbstractTypeApproximator

class InferenceComponents(val session: FirSession) {
    val ctx: ConeInferenceContext = ConeTypeCheckerContext(isErrorTypeEqualsToAnything = false, isStubTypeEqualsToAnything = false, session)

    val approximator: AbstractTypeApproximator = object : AbstractTypeApproximator(ctx) {}
    val trivialConstraintTypeInferenceOracle = TrivialConstraintTypeInferenceOracle.create(ctx)
    private val incorporator = ConstraintIncorporator(approximator, trivialConstraintTypeInferenceOracle, ConeConstraintSystemUtilContext)
    private val injector = ConstraintInjector(incorporator, approximator)
    val resultTypeResolver = ResultTypeResolver(approximator, trivialConstraintTypeInferenceOracle)

    val constraintSystemFactory = ConstraintSystemFactory()

    fun createConstraintSystem(): NewConstraintSystemImpl {
        return NewConstraintSystemImpl(injector, ctx)
    }

    inner class ConstraintSystemFactory {
        fun createConstraintSystem(): NewConstraintSystemImpl {
            return this@InferenceComponents.createConstraintSystem()
        }
    }
}
