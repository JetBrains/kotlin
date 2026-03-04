/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.create
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.test.model.ResultingArtifact
import org.jetbrains.kotlin.test.model.TestArtifactKind
import org.jetbrains.kotlin.test.services.KotlinTestInfo
import org.jetbrains.kotlin.test.services.TestService
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.testInfo

/**
 * Prepares the input artifact for the grouping phase by merging non-grouping phase outputs.
 *
 * The services in the WIP state and could be changed in the future
 */
class GroupingPhaseInputsMerger(val testServices: TestServices, val workers: List<Worker>) {
    fun merge(nonGroupingPhaseOutputs: List<NonGroupingPhaseOutput>): GroupingPhaseInputArtifact {
        val secondPhaseConfiguration = CompilerConfiguration.create(messageCollector = MessageCollector.NONE)
        workers.forEach { worker ->
            worker.process(secondPhaseConfiguration, nonGroupingPhaseOutputs.map { it.testServices })
        }
        return GroupingPhaseInputArtifact(secondPhaseConfiguration, nonGroupingPhaseOutputs)
    }

    /**
     * Single unit of an artifact merging processing. Several workers could be registered in the test configuration.
     */
    abstract class Worker(val testServices: TestServices) {
        abstract fun process(configuration: CompilerConfiguration, firstPhaseServices: List<TestServices>)
    }
}

data class NonGroupingPhaseOutput(
    val testServices: TestServices,
    val catchingExecutor: CatchingExecutor,
) {
    val testInfo: KotlinTestInfo get() = testServices.testInfo

    /**
     * Allows executing code which potentially throws an exception during the grouping phase, so this exception
     * would be reported as a failure of the single test, not the whole group. The actual implementation is provided
     * by the test engine.
     */
    fun interface CatchingExecutor {
        fun executeWithCatching(block: () -> Unit)
    }
}


class GroupingPhaseInputArtifact(
    val secondPhaseConfiguration: CompilerConfiguration,
    val nonGroupingPhaseOutputs: List<NonGroupingPhaseOutput>
) : ResultingArtifact<GroupingPhaseInputArtifact>() {
    object Kind : TestArtifactKind<GroupingPhaseInputArtifact>("SecondPhaseInputArtifact")

    override val kind: Kind get() = Kind
}

class GroupingPhaseInputsHolder(val nonGroupingPhaseOutputs: List<NonGroupingPhaseOutput>) : TestService

private val TestServices.groupingPhaseInputsHolder: GroupingPhaseInputsHolder by TestServices.testServiceAccessor()
val TestServices.groupingPhaseInputs: List<NonGroupingPhaseOutput>
    get() = groupingPhaseInputsHolder.nonGroupingPhaseOutputs
