/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.utils.bind

sealed class TestStepBuilder<InputArtifact, OutputArtifact>
        where InputArtifact : ResultingArtifact<InputArtifact>,
              OutputArtifact : ResultingArtifact<OutputArtifact> {
    @TestInfrastructureInternals
    abstract fun createTestStep(testServices: TestServices): TestStep<InputArtifact, OutputArtifact>
}


class FacadeStepBuilder<InputArtifact, OutputArtifact>(
    val facade: Constructor<AbstractTestFacade<InputArtifact, OutputArtifact>>,
) : TestStepBuilder<InputArtifact, OutputArtifact>()
        where InputArtifact : ResultingArtifact<InputArtifact>,
              OutputArtifact : ResultingArtifact<OutputArtifact> {
    @TestInfrastructureInternals
    override fun createTestStep(testServices: TestServices): TestStep.FacadeStep<InputArtifact, OutputArtifact> {
        return TestStep.FacadeStep(facade.invoke(testServices))
    }
}

class HandlersStepBuilder<InputArtifact, InputArtifactKind>(val artifactKind: InputArtifactKind) :
    TestStepBuilder<InputArtifact, Nothing>()
        where InputArtifact : ResultingArtifact<InputArtifact>,
              InputArtifactKind : TestArtifactKind<InputArtifact> {
    private val handlers: MutableList<Constructor<AnalysisHandler<InputArtifact>>> = mutableListOf()

    fun useHandlers(vararg constructor: Constructor<AnalysisHandler<InputArtifact>>) {
        handlers += constructor
    }

    fun useHandlers(vararg constructor: Constructor2<InputArtifactKind, AnalysisHandler<InputArtifact>>) {
        constructor.mapTo(handlers) { it.bind(artifactKind) }
    }

    fun useHandlersAtFirst(vararg constructor: Constructor<AnalysisHandler<InputArtifact>>) {
        handlers.addAll(0, constructor.toList())
    }

    fun useHandlers(constructors: List<Constructor<AnalysisHandler<InputArtifact>>>) {
        handlers += constructors
    }

    @TestInfrastructureInternals
    override fun createTestStep(testServices: TestServices): TestStep.HandlersStep<InputArtifact> {
        return TestStep.HandlersStep(artifactKind, handlers.map { it.invoke(testServices) })
    }
}
