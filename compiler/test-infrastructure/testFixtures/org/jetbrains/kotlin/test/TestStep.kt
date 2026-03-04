/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import org.jetbrains.kotlin.test.NonGroupingTestRunner.Companion.shouldRun
import org.jetbrains.kotlin.test.model.*

sealed class TestStep<InputArtifact, OutputArtifact>
        where InputArtifact : ResultingArtifact<InputArtifact>,
              OutputArtifact : ResultingArtifact<OutputArtifact> {
    abstract val inputArtifactKind: TestArtifactKind<InputArtifact>

    sealed interface FacadeStep<InputArtifact, OutputArtifact>
            where InputArtifact : ResultingArtifact<InputArtifact>,
                  OutputArtifact : ResultingArtifact<OutputArtifact> {
        val facade: AbstractTestFacadeBase<InputArtifact, OutputArtifact>
    }

    sealed interface HandlersStep<InputArtifact>
            where InputArtifact : ResultingArtifact<InputArtifact>{
        val handlers: List<AnalysisHandlerBase<InputArtifact>>
    }

    sealed class NonGroupingStep<InputArtifact, OutputArtifact> : TestStep<InputArtifact, OutputArtifact>()
            where InputArtifact : ResultingArtifact<InputArtifact>,
                  OutputArtifact : ResultingArtifact<OutputArtifact> {

        open fun shouldProcessModule(module: TestModule, inputArtifact: ResultingArtifact<*>): Boolean {
            return inputArtifact.kind == inputArtifactKind
        }

        abstract fun processModule(
            module: TestModule,
            inputArtifact: InputArtifact,
            thereWereExceptionsOnPreviousSteps: Boolean,
        ): StepResult<out OutputArtifact>

        class FacadeStep<InputArtifact, OutputArtifact>(
            override val facade: AbstractTestFacade<InputArtifact, OutputArtifact>,
        ) : NonGroupingStep<InputArtifact, OutputArtifact>(), TestStep.FacadeStep<InputArtifact, OutputArtifact>
                where InputArtifact : ResultingArtifact<InputArtifact>,
                      OutputArtifact : ResultingArtifact<OutputArtifact> {
            override val inputArtifactKind: TestArtifactKind<InputArtifact>
                get() = facade.inputKind

            override fun shouldProcessModule(module: TestModule, inputArtifact: ResultingArtifact<*>): Boolean {
                return super.shouldProcessModule(module, inputArtifact) && facade.shouldTransform(module)
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
                    return StepResult.ErrorFromFacade(WrappedException.FromFacade(e, module, facade))
                }
                return StepResult.Artifact(outputArtifact)
            }

            override fun toString(): String {
                return "Facade: ${facade::class.simpleName}"
            }
        }

        class HandlersStep<InputArtifact : ResultingArtifact<InputArtifact>>(
            override val inputArtifactKind: TestArtifactKind<InputArtifact>,
            override val handlers: List<AnalysisHandler<InputArtifact>>
        ) : NonGroupingStep<InputArtifact, Nothing>(), TestStep.HandlersStep<InputArtifact> {
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
                var shouldRunNextSteps = true
                for (outputHandler in handlers) {
                    if (outputHandler.shouldRun(thereWasAnException = thereWereExceptionsOnPreviousSteps || exceptions.isNotEmpty())) {
                        try {
                            outputHandler.processModule(module, inputArtifact)
                        } catch (e: Throwable) {
                            exceptions += WrappedException.FromHandler(e, module, outputHandler)
                            if (outputHandler.failureDisablesNextSteps) {
                                shouldRunNextSteps = false
                            }
                        }
                    }
                }
                return StepResult.HandlersResult(exceptions, shouldRunNextSteps)
            }

            override fun toString(): String {
                return "Handlers for $inputArtifactKind"
            }
        }
    }

    sealed class GroupingPhaseStep<InputArtifact, OutputArtifact> : TestStep<InputArtifact, OutputArtifact>()
            where InputArtifact : ResultingArtifact<InputArtifact>,
                  OutputArtifact : ResultingArtifact<OutputArtifact> {

        abstract fun process(inputArtifact: InputArtifact, thereWereExceptionsOnPreviousSteps: Boolean): StepResult<out OutputArtifact>

        class FacadeStep<InputArtifact, OutputArtifact>(
            override val facade: AbstractGroupingPhaseTestFacade<InputArtifact, OutputArtifact>,
        ) : GroupingPhaseStep<InputArtifact, OutputArtifact>(), TestStep.FacadeStep<InputArtifact, OutputArtifact>
                where InputArtifact : ResultingArtifact<InputArtifact>,
                      OutputArtifact : ResultingArtifact<OutputArtifact> {
            override val inputArtifactKind: TestArtifactKind<InputArtifact>
                get() = facade.inputKind


            override fun process(
                inputArtifact: InputArtifact,
                thereWereExceptionsOnPreviousSteps: Boolean,
            ): StepResult<out OutputArtifact> {
                val outputArtifact = try {
                    facade.transform(inputArtifact) ?: return StepResult.NoArtifactFromFacade
                } catch (e: Throwable) {
                    return StepResult.ErrorFromFacade(WrappedException.FromGroupingFacade(e, facade))
                }
                return StepResult.Artifact(outputArtifact)
            }

            override fun toString(): String {
                return "Facade: ${facade::class.simpleName}"
            }
        }

        class HandlersStep<InputArtifact : ResultingArtifact<InputArtifact>>(
            override val inputArtifactKind: TestArtifactKind<InputArtifact>,
            override val handlers: List<GroupingPhaseHandler<InputArtifact>>
        ) : GroupingPhaseStep<InputArtifact, Nothing>(), TestStep.HandlersStep<InputArtifact> {
            init {
                for (handler in handlers) {
                    require(handler.artifactKind == inputArtifactKind) {
                        "Artifact kind mismatch. Artifact kind of each handler must match input artifact kind ($inputArtifactKind). " +
                                "In handler $handler artifact kind is ${handler.artifactKind}"
                    }
                }
            }

            override fun process(
                inputArtifact: InputArtifact,
                thereWereExceptionsOnPreviousSteps: Boolean,
            ): StepResult.HandlersResult {
                val exceptions = mutableListOf<WrappedException>()
                var shouldRunNextSteps = true
                for (outputHandler in handlers) {
                    try {
                        outputHandler.processArtifact(inputArtifact)
                    } catch (e: Throwable) {
                        exceptions += WrappedException.FromGroupingHandler(e, outputHandler)
                        if (outputHandler.failureDisablesNextSteps) {
                            shouldRunNextSteps = false
                        }
                    }
                }
                return StepResult.HandlersResult(exceptions, shouldRunNextSteps)
            }

            override fun toString(): String {
                return "Handlers for $inputArtifactKind"
            }
        }
    }

    sealed class StepResult<OutputArtifact : ResultingArtifact<OutputArtifact>> {

        class Artifact<OutputArtifact : ResultingArtifact<OutputArtifact>>(val outputArtifact: OutputArtifact) :
            StepResult<OutputArtifact>()

        class ErrorFromFacade<OutputArtifact : ResultingArtifact<OutputArtifact>>(val exception: WrappedException) :
            StepResult<OutputArtifact>()

        data class HandlersResult(
            val exceptionsFromHandlers: Collection<WrappedException>,
            val shouldRunNextSteps: Boolean
        ) : StepResult<Nothing>()

        data object NoArtifactFromFacade : StepResult<Nothing>()
    }
}


