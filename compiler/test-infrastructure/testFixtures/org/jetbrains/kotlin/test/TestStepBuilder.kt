/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.CompilationStage
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.utils.bind

sealed class TestStepBuilder<InputArtifact, OutputArtifact, out FacadeStep>
        where InputArtifact : ResultingArtifact<InputArtifact>,
              OutputArtifact : ResultingArtifact<OutputArtifact>,
              FacadeStep : TestStep<InputArtifact, OutputArtifact> {
    @TestInfrastructureInternals
    abstract fun createTestStep(testServices: TestServices): FacadeStep

    sealed class FacadeStepBuilder<InputArtifact, OutputArtifact, Facade, FacadeStep>(
        val facade: Constructor<Facade>,
    ) : TestStepBuilder<InputArtifact, OutputArtifact, FacadeStep>()
            where InputArtifact : ResultingArtifact<InputArtifact>,
                  OutputArtifact : ResultingArtifact<OutputArtifact>,
                  Facade : AbstractTestFacadeBase<InputArtifact, OutputArtifact>,
                  FacadeStep : TestStep<InputArtifact, OutputArtifact> {
        @TestInfrastructureInternals
        abstract override fun createTestStep(testServices: TestServices): FacadeStep

        class NonGroupingStage<InputArtifact, OutputArtifact>(
            facade: Constructor<AbstractTestFacade<InputArtifact, OutputArtifact>>,
        ) : FacadeStepBuilder<
                InputArtifact,
                OutputArtifact,
                AbstractTestFacade<InputArtifact, OutputArtifact>,
                TestStep.NonGroupingStep.FacadeStep<InputArtifact, OutputArtifact>
                >(facade) where InputArtifact : ResultingArtifact<InputArtifact>,
                                OutputArtifact : ResultingArtifact<OutputArtifact> {
            @TestInfrastructureInternals
            override fun createTestStep(testServices: TestServices): TestStep.NonGroupingStep.FacadeStep<InputArtifact, OutputArtifact> {
                return TestStep.NonGroupingStep.FacadeStep(facade.invoke(testServices))
            }
        }

        class GroupingStage<InputArtifact, OutputArtifact>(
            facade: Constructor<AbstractGroupingStageTestFacade<InputArtifact, OutputArtifact>>,
        ) : FacadeStepBuilder<
                InputArtifact,
                OutputArtifact,
                AbstractGroupingStageTestFacade<InputArtifact, OutputArtifact>,
                TestStep.GroupingStageStep.FacadeStep<InputArtifact, OutputArtifact>
                >(facade) where InputArtifact : ResultingArtifact<InputArtifact>,
                                OutputArtifact : ResultingArtifact<OutputArtifact> {
            @TestInfrastructureInternals
            override fun createTestStep(testServices: TestServices): TestStep.GroupingStageStep.FacadeStep<InputArtifact, OutputArtifact> {
                return TestStep.GroupingStageStep.FacadeStep(facade.invoke(testServices))
            }
        }
    }

    sealed class HandlersStepBuilder<InputArtifact, InputArtifactKind, Handler, HandlersStep>(
        val artifactKind: InputArtifactKind,
        val compilationStage: CompilationStage,
    ) : TestStepBuilder<InputArtifact, Nothing, HandlersStep>()
            where InputArtifact : ResultingArtifact<InputArtifact>,
                  InputArtifactKind : TestArtifactKind<InputArtifact>,
                  Handler : AnalysisHandlerBase<InputArtifact>,
                  HandlersStep : TestStep<InputArtifact, Nothing> {
        private val handlers: MutableList<Constructor<Handler>> = mutableListOf()

        fun useHandlers(vararg constructor: Constructor<Handler>) {
            handlers += constructor
        }

        fun useHandlers(vararg constructor: Constructor2<InputArtifactKind, Handler>) {
            constructor.mapTo(handlers) { it.bind(artifactKind) }
        }

        fun useHandlersAtFirst(vararg constructor: Constructor<Handler>) {
            handlers.addAll(0, constructor.toList())
        }

        fun useHandlers(constructors: List<Constructor<Handler>>) {
            handlers += constructors
        }

        @TestInfrastructureInternals
        override fun createTestStep(testServices: TestServices): HandlersStep {
            val handlers = handlers.map { constructor ->
                constructor
                    .invoke(testServices)
                    .also { it.setCompilationStage(compilationStage) }
            }
            return createStep(handlers)
        }

        protected abstract fun createStep(handlers: List<Handler>): HandlersStep

        class NonGroupingStage<InputArtifact, InputArtifactKind>(
            artifactKind: InputArtifactKind,
            compilationStage: CompilationStage,
        ) : HandlersStepBuilder<
                InputArtifact,
                InputArtifactKind,
                AnalysisHandler<InputArtifact>,
                TestStep.NonGroupingStep.HandlersStep<InputArtifact>>
            (artifactKind, compilationStage)
                where InputArtifact : ResultingArtifact<InputArtifact>,
                      InputArtifactKind : TestArtifactKind<InputArtifact> {
            override fun createStep(handlers: List<AnalysisHandler<InputArtifact>>): TestStep.NonGroupingStep.HandlersStep<InputArtifact> {
                return TestStep.NonGroupingStep.HandlersStep(artifactKind, handlers)
            }
        }

        class GroupingStage<InputArtifact, InputArtifactKind>(
            artifactKind: InputArtifactKind,
            compilationStage: CompilationStage,
        ) : HandlersStepBuilder<
                InputArtifact,
                InputArtifactKind,
                GroupingStageHandler<InputArtifact>,
                TestStep.GroupingStageStep.HandlersStep<InputArtifact>>
            (artifactKind, compilationStage)
                where InputArtifact : ResultingArtifact<InputArtifact>,
                      InputArtifactKind : TestArtifactKind<InputArtifact> {
            override fun createStep(handlers: List<GroupingStageHandler<InputArtifact>>): TestStep.GroupingStageStep.HandlersStep<InputArtifact> {
                return TestStep.GroupingStageStep.HandlersStep(artifactKind, handlers)
            }
        }
    }
}
