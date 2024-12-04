/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.NoMutableState
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.types.ConeInferenceContext
import org.jetbrains.kotlin.fir.types.typeApproximator
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.resolve.calls.inference.components.*
import org.jetbrains.kotlin.resolve.calls.inference.model.NewConstraintSystemImpl

@NoMutableState
class InferenceComponents(val session: FirSession) : FirSessionComponent {
    private val typeContext: ConeInferenceContext = session.typeContext
    private val approximator = session.typeApproximator

    val trivialConstraintTypeInferenceOracle: TrivialConstraintTypeInferenceOracle = TrivialConstraintTypeInferenceOracle.create(typeContext)
    private val incorporator = ConstraintIncorporator(approximator, trivialConstraintTypeInferenceOracle, ConeConstraintSystemUtilContext)
    private val injector = ConstraintInjector(
        incorporator,
        approximator,
        session.languageVersionSettings,
    )
    val resultTypeResolver: ResultTypeResolver =
        ResultTypeResolver(approximator, trivialConstraintTypeInferenceOracle, session.languageVersionSettings)
    val variableFixationFinder: VariableFixationFinder =
        VariableFixationFinder(trivialConstraintTypeInferenceOracle, session.languageVersionSettings)
    val postponedArgumentInputTypesResolver: PostponedArgumentInputTypesResolver =
        PostponedArgumentInputTypesResolver(
            resultTypeResolver, variableFixationFinder, ConeConstraintSystemUtilContext, session.languageVersionSettings
        )

    val constraintSystemFactory: ConstraintSystemFactory = ConstraintSystemFactory()

    fun createConstraintSystem(): NewConstraintSystemImpl {
        return NewConstraintSystemImpl(injector, typeContext, session.languageVersionSettings)
    }

    inner class ConstraintSystemFactory {
        fun createConstraintSystem(): NewConstraintSystemImpl {
            return this@InferenceComponents.createConstraintSystem()
        }
    }
}

val FirSession.inferenceComponents: InferenceComponents by FirSession.sessionComponentAccessor()
