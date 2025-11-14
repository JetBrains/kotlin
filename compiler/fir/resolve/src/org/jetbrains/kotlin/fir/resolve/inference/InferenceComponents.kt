/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.NoMutableState
import org.jetbrains.kotlin.fir.SessionHolder
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.types.ConeInferenceContext
import org.jetbrains.kotlin.fir.types.typeApproximator
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.resolve.calls.inference.components.*
import org.jetbrains.kotlin.resolve.calls.inference.model.NewConstraintSystemImpl

@NoMutableState
class InferenceComponents(override val session: FirSession) : FirSessionComponent, SessionHolder {
    private val typeContext: ConeInferenceContext = session.typeContext
    private val approximator = session.typeApproximator

    val trivialConstraintTypeInferenceOracle: TrivialConstraintTypeInferenceOracle =
        TrivialConstraintTypeInferenceOracle.create(typeContext)
    private val incorporator =
        ConstraintIncorporator(
            approximator,
            trivialConstraintTypeInferenceOracle,
            ConeConstraintSystemUtilContext,
            session.languageVersionSettings,
            session.inferenceLogger,
        )
    private val injector = ConstraintInjector(
        incorporator,
        approximator,
        session.languageVersionSettings,
        session.inferenceLogger,
    )
    val resultTypeResolver: ResultTypeResolver =
        ResultTypeResolver(approximator, trivialConstraintTypeInferenceOracle, session.languageVersionSettings)
    val variableFixationFinder: VariableFixationFinder = run {
        val variableReadinessCalculatorBuilder =
            ::VariableReadinessCalculator.takeIf { LanguageFeature.LexicographicVariableReadinessCalculation.isEnabled() }
                ?: ::LegacyVariableReadinessCalculator

        VariableFixationFinder.Default(
            session.languageVersionSettings,
            variableReadinessCalculatorBuilder(
                trivialConstraintTypeInferenceOracle,
                session.languageVersionSettings,
                session.inferenceLogger,
            ),
        )
    }
    val postponedArgumentInputTypesResolver: PostponedArgumentInputTypesResolver =
        PostponedArgumentInputTypesResolver(
            resultTypeResolver, variableFixationFinder, ConeConstraintSystemUtilContext
        )

    val constraintSystemFactory: ConstraintSystemFactory = ConstraintSystemFactory()

    fun createConstraintSystem(): NewConstraintSystemImpl {
        return NewConstraintSystemImpl(
            injector, typeContext,
            session.languageVersionSettings,
        )
    }

    inner class ConstraintSystemFactory {
        fun createConstraintSystem(): NewConstraintSystemImpl {
            return this@InferenceComponents.createConstraintSystem()
        }
    }
}

val FirSession.inferenceComponents: InferenceComponents by FirSession.sessionComponentAccessor()
