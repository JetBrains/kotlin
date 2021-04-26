/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference

import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.types.ConeInferenceContext
import org.jetbrains.kotlin.fir.types.ConeTypeApproximator
import org.jetbrains.kotlin.resolve.calls.inference.components.*
import org.jetbrains.kotlin.resolve.calls.inference.model.NewConstraintSystemImpl

@NoMutableState
class InferenceComponents(val session: FirSession) : FirSessionComponent {
    val ctx: ConeInferenceContext = object : ConeInferenceContext {
        override val session: FirSession
            get() = this@InferenceComponents.session
    }

    val approximator: ConeTypeApproximator = ConeTypeApproximator(ctx, session.languageVersionSettings)
    val trivialConstraintTypeInferenceOracle = TrivialConstraintTypeInferenceOracle.create(ctx)
    private val incorporator = ConstraintIncorporator(approximator, trivialConstraintTypeInferenceOracle, ConeConstraintSystemUtilContext)
    private val injector = ConstraintInjector(
        incorporator,
        approximator,
        session.languageVersionSettings,
    )
    val resultTypeResolver = ResultTypeResolver(approximator, trivialConstraintTypeInferenceOracle)
    val variableFixationFinder = VariableFixationFinder(trivialConstraintTypeInferenceOracle, session.languageVersionSettings)
    val postponedArgumentInputTypesResolver =
        PostponedArgumentInputTypesResolver(resultTypeResolver, variableFixationFinder, ConeConstraintSystemUtilContext)

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

val FirSession.inferenceComponents: InferenceComponents by FirSession.sessionComponentAccessor()
