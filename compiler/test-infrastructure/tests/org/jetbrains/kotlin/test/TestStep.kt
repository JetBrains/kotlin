/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import org.jetbrains.kotlin.test.TestRunner.Companion.shouldRun
import org.jetbrains.kotlin.test.model.*

sealed class TestStep<InputArtifact, OutputArtifact>
        where InputArtifact : ResultingArtifact<InputArtifact>,
              OutputArtifact : ResultingArtifact<OutputArtifact> {
    abstract val inputArtifactKind: TestArtifactKind<InputArtifact>

    open fun shouldProcessModule(module: TestModule, inputArtifact: ResultingArtifact<*>): Boolean {
        return inputArtifact.kind == inputArtifactKind
    }

    abstract fun processModule(
        module: TestModule,
        inputArtifact: InputArtifact,
        thereWereExceptionsOnPreviousSteps: Boolean,
    ): StepResult<out OutputArtifact>

    class FacadeStep<InputArtifact, OutputArtifact>(
        val facade: AbstractTestFacade<InputArtifact, OutputArtifact>,
    ) : TestStep<InputArtifact, OutputArtifact>()
            where InputArtifact : ResultingArtifact<InputArtifact>,
                  OutputArtifact : ResultingArtifact<OutputArtifact> {
        override val inputArtifactKind: TestArtifactKind<InputArtifact>
            get() = facade.inputKind

        val outputArtifactKind: TestArtifactKind<OutputArtifact>
            get() = facade.outputKind

        override fun shouldProcessModule(module: TestModule, inputArtifact: ResultingArtifact<*>): Boolean {
            return super.shouldProcessModule(module, inputArtifact) && facade.shouldRunAnalysis(module)
        }

        override fun processModule(
            module: TestModule,
            inputArtifact: InputArtifact,
            thereWereExceptionsOnPreviousSteps: Boolean,
        ): StepResult<out OutputArtifact> {
            val outputArtifact = try {
                facade.transform(module, inputArtifact) ?: return StepResult.NoArtifactFromFacade
            } catch (e: Throwable) {
                // TODO: remove inheritors of WrappedException.FromFacade
                return StepResult.ErrorFromFacade(WrappedException.FromFacade(e, facade))
            }
            return StepResult.Artifact(outputArtifact)
        }
    }

    class HandlersStep<InputArtifact : ResultingArtifact<InputArtifact>>(
        override val inputArtifactKind: TestArtifactKind<InputArtifact>,
        val handlers: List<AnalysisHandler<InputArtifact>>
    ) : TestStep<InputArtifact, Nothing>() {
        init {
            for (handler in handlers) {
                require(handler.artifactKind == inputArtifactKind) {
                    "Artifact kind mismatch. Artifact kind of each handler must match input artifact kind ($inputArtifactKind). " +
                            "In handler $handler artifact kind is ${handler.artifactKind}"
                }
            }
        }

        override fun processModule(
            module: TestModule,
            inputArtifact: InputArtifact,
            thereWereExceptionsOnPreviousSteps: Boolean
        ): StepResult.HandlersResult {
            val exceptions = mutableListOf<WrappedException>()
            val ranHandlers = mutableSetOf<AnalysisHandler<*>>()
            for (outputHandler in handlers) {
                if (outputHandler.shouldRun(thereWasAnException = thereWereExceptionsOnPreviousSteps || exceptions.isNotEmpty())) {
                    try {
                        outputHandler.processModule(module, inputArtifact)
                    } catch (e: Throwable) {
                        exceptions += WrappedException.FromHandler(e, outputHandler)
                        if (outputHandler.failureDisablesNextSteps) {
                            return StepResult.HandlersResult(exceptions, handlers, shouldRunNextSteps = false)
                        }
                    }
                    ranHandlers.add(outputHandler)
                }
            }
            return StepResult.HandlersResult(exceptions, ranHandlers, shouldRunNextSteps = true)
        }
    }

    sealed class StepResult<OutputArtifact : ResultingArtifact<OutputArtifact>> {

        class Artifact<OutputArtifact : ResultingArtifact<OutputArtifact>>(val outputArtifact: OutputArtifact) :
            StepResult<OutputArtifact>()

        class ErrorFromFacade<OutputArtifact : ResultingArtifact<OutputArtifact>>(val exception: WrappedException) :
            StepResult<OutputArtifact>()

        data class HandlersResult(
            val exceptionsFromHandlers: Collection<WrappedException>,
            val ranHandlers: Collection<AnalysisHandler<*>>,
            val shouldRunNextSteps: Boolean
        ) : StepResult<Nothing>()

        data object NoArtifactFromFacade : StepResult<Nothing>()
    }
}


