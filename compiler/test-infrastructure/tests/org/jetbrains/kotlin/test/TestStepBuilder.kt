/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.TestServices

sealed class TestStepBuilder<I : ResultingArtifact<I>, O : ResultingArtifact<O>> {
    @TestInfrastructureInternals
    abstract fun createTestStep(testServices: TestServices): TestStep<I, O>
}


class FacadeStepBuilder<I : ResultingArtifact<I>, O : ResultingArtifact<O>>(
    val facade: Constructor<AbstractTestFacade<I, O>>
) : TestStepBuilder<I, O>() {
    @TestInfrastructureInternals
    override fun createTestStep(testServices: TestServices): TestStep.FacadeStep<I, O> {
        return TestStep.FacadeStep(facade.invoke(testServices))
    }
}

class HandlersStepBuilder<I : ResultingArtifact<I>>(val artifactKind: TestArtifactKind<I>) : TestStepBuilder<I, Nothing>() {
    private val handlers: MutableList<Constructor<AnalysisHandler<I>>> = mutableListOf()

    fun useHandlers(vararg constructor: Constructor<AnalysisHandler<I>>) {
        handlers += constructor
    }

    fun useHandlersAtFirst(vararg constructor: Constructor<AnalysisHandler<I>>) {
        handlers.addAll(0, constructor.toList())
    }

    fun useHandlers(constructors: List<Constructor<AnalysisHandler<I>>>) {
        handlers += constructors
    }

    @TestInfrastructureInternals
    override fun createTestStep(testServices: TestServices): TestStep.HandlersStep<I> {
        return TestStep.HandlersStep(artifactKind, handlers.map { it.invoke(testServices) })
    }
}
