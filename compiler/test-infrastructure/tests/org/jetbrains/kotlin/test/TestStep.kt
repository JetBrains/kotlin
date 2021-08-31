/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import org.jetbrains.kotlin.test.TestRunner.Companion.shouldRun
import org.jetbrains.kotlin.test.model.*

sealed class TestStep<I : ResultingArtifact<I>, O : ResultingArtifact<O>> {
    abstract val inputArtifactKind: TestArtifactKind<I>

    open fun shouldProcessModule(module: TestModule, inputArtifact: ResultingArtifact<*>): Boolean {
        return inputArtifact.kind == inputArtifactKind
    }

    abstract fun processModule(module: TestModule, inputArtifact: I, thereWereExceptionsOnPreviousSteps: Boolean): StepResult<out O>

    class FacadeStep<I : ResultingArtifact<I>, O : ResultingArtifact<O>>(val facade: AbstractTestFacade<I, O>) : TestStep<I, O>() {
        override val inputArtifactKind: TestArtifactKind<I>
            get() = facade.inputKind

        val outputArtifactKind: TestArtifactKind<O>
            get() = facade.outputKind

        override fun shouldProcessModule(module: TestModule, inputArtifact: ResultingArtifact<*>): Boolean {
            return super.shouldProcessModule(module, inputArtifact) && facade.shouldRunAnalysis(module)
        }

        override fun processModule(module: TestModule, inputArtifact: I, thereWereExceptionsOnPreviousSteps: Boolean): StepResult<out O> {
            val outputArtifact = try {
                facade.transform(module, inputArtifact) ?: return StepResult.NoArtifactFromFacade
            } catch (e: Throwable) {
                // TODO: remove inheritors of WrappedException.FromFacade
                return StepResult.ErrorFromFacade(WrappedException.FromFacade(e, facade))
            }
            return StepResult.Artifact(outputArtifact)
        }
    }

    class HandlersStep<I : ResultingArtifact<I>>(
        override val inputArtifactKind: TestArtifactKind<I>,
        val handlers: List<AnalysisHandler<I>>
    ) : TestStep<I, Nothing>() {
        init {
            require(handlers.all { it.artifactKind == inputArtifactKind })
        }

        override fun processModule(
            module: TestModule,
            inputArtifact: I,
            thereWereExceptionsOnPreviousSteps: Boolean
        ): StepResult.HandlersResult {
            val exceptions = mutableListOf<WrappedException>()
            for (outputHandler in handlers) {
                if (outputHandler.shouldRun(thereWasAnException = thereWereExceptionsOnPreviousSteps || exceptions.isNotEmpty())) {
                    try {
                        outputHandler.processModule(module, inputArtifact)
                    } catch (e: Throwable) {
                        exceptions += WrappedException.FromHandler(e, outputHandler)
                        if (outputHandler.failureDisablesNextSteps) {
                            return StepResult.HandlersResult(exceptions, shouldRunNextSteps = false)
                        }
                    }
                }
            }
            return StepResult.HandlersResult(exceptions, shouldRunNextSteps = true)
        }
    }

    sealed class StepResult<O : ResultingArtifact<O>> {
        class Artifact<O : ResultingArtifact<O>>(val outputArtifact: O) : StepResult<O>()
        class ErrorFromFacade<O : ResultingArtifact<O>>(val exception: WrappedException) : StepResult<O>()
        data class HandlersResult(
            val exceptionsFromHandlers: Collection<WrappedException>,
            val shouldRunNextSteps: Boolean
        ) : StepResult<Nothing>()

        object NoArtifactFromFacade : StepResult<Nothing>()
    }
}


