/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import org.jetbrains.kotlin.test.NonGroupingTestRunner.Companion.shouldRun
import org.jetbrains.kotlin.test.model.*

/**
 * Defines which artifact [NonGroupingTestRunner] passes to a facade step.
 *
 * The test pipeline is an ordered list of steps, and the runner keeps track of the artifact produced by the last
 * executed facade (the *latest* artifact). A facade step always prefers the latest artifact, and by default
 * ([AnyArtifactOfInputKind]) may also reach back to an artifact produced earlier in the pipeline, which allows building
 * branching pipelines, e.g.:
 *
 * ```
 * sources -> fir -> ir -> klib -> deserialized ir -> compiled js
 *                                                 -> exported ts
 * ```
 */
enum class FacadeInputArtifactSelection {
    /**
     * Strictly linear behavior: the facade step is executed only if the artifact produced by the previous facade matches
     * [AbstractTestFacadeBase.inputKind]. Otherwise the step is skipped.
     *
     * This is an opt-out from the default [AnyArtifactOfInputKind] for steps which must never see anything but the
     * immediately preceding artifact.
     */
    LatestArtifactOnly,

    /**
     * The default behavior.
     *
     * If the latest artifact doesn't match [AbstractTestFacadeBase.inputKind], the runner looks up the most recent
     * artifact of that kind produced earlier in the pipeline
     * (see [org.jetbrains.kotlin.test.services.ArtifactsProvider]).
     *
     * Note that the *starting* artifact of the pipeline is not a part of that lookup: once the pipeline has moved past
     * it, facades consuming it (e.g. frontend facades) are never re-entered. Several pipelines rely on that, e.g. the
     * Kotlin/Native one, where `ObjCInteropFacade` compiles `.def` modules straight to a KLIB and the whole
     * source-consuming part of the pipeline must be skipped for such modules.
     */
    AnyArtifactOfInputKind,
}

sealed class TestStep<InputArtifact, OutputArtifact>
        where InputArtifact : ResultingArtifact<InputArtifact>,
              OutputArtifact : ResultingArtifact<OutputArtifact> {
    abstract val inputArtifactKind: TestArtifactKind<InputArtifact>

    /**
     * The kind of the artifact produced by this step, or `null` if the step produces nothing (i.e. it's a handlers step).
     */
    abstract val outputArtifactKind: TestArtifactKind<*>?

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

        abstract fun processModule(
            module: TestModule,
            inputArtifact: InputArtifact,
            thereWereExceptionsOnPreviousSteps: Boolean,
        ): StepResult<out OutputArtifact>

        class FacadeStep<InputArtifact, OutputArtifact>(
            override val facade: AbstractTestFacade<InputArtifact, OutputArtifact>,
            val inputArtifactSelection: FacadeInputArtifactSelection,
        ) : NonGroupingStep<InputArtifact, OutputArtifact>(), TestStep.FacadeStep<InputArtifact, OutputArtifact>
                where InputArtifact : ResultingArtifact<InputArtifact>,
                      OutputArtifact : ResultingArtifact<OutputArtifact> {
            override val inputArtifactKind: TestArtifactKind<InputArtifact>
                get() = facade.inputKind

            override val outputArtifactKind: TestArtifactKind<OutputArtifact>
                get() = facade.outputKind

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
                    checkTestInfrastructure(handler.artifactKind == inputArtifactKind) {
                        "Artifact kind mismatch. Artifact kind of each handler must match input artifact kind ($inputArtifactKind). " +
                                "In handler $handler artifact kind is ${handler.artifactKind}"
                    }
                }
            }

            override val outputArtifactKind: TestArtifactKind<*>? get() = null

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

    sealed class GroupingStageStep<InputArtifact, OutputArtifact> : TestStep<InputArtifact, OutputArtifact>()
            where InputArtifact : ResultingArtifact<InputArtifact>,
                  OutputArtifact : ResultingArtifact<OutputArtifact> {

        abstract fun process(inputArtifact: InputArtifact, thereWereExceptionsOnPreviousSteps: Boolean): StepResult<out OutputArtifact>

        class FacadeStep<InputArtifact, OutputArtifact>(
            override val facade: AbstractGroupingStageTestFacade<InputArtifact, OutputArtifact>,
        ) : GroupingStageStep<InputArtifact, OutputArtifact>(), TestStep.FacadeStep<InputArtifact, OutputArtifact>
                where InputArtifact : ResultingArtifact<InputArtifact>,
                      OutputArtifact : ResultingArtifact<OutputArtifact> {
            override val inputArtifactKind: TestArtifactKind<InputArtifact>
                get() = facade.inputKind

            override val outputArtifactKind: TestArtifactKind<OutputArtifact>
                get() = facade.outputKind

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
            override val handlers: List<GroupingStageHandler<InputArtifact>>
        ) : GroupingStageStep<InputArtifact, Nothing>(), TestStep.HandlersStep<InputArtifact> {
            init {
                for (handler in handlers) {
                    checkTestInfrastructure(handler.artifactKind == inputArtifactKind) {
                        "Artifact kind mismatch. Artifact kind of each handler must match input artifact kind ($inputArtifactKind). " +
                                "In handler $handler artifact kind is ${handler.artifactKind}"
                    }
                }
            }

            override val outputArtifactKind: TestArtifactKind<*>? get() = null

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
