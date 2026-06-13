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
 * Prepares the input artifact for the grouping stage by merging non-grouping stage outputs.
 *
 * The services in the WIP state and could be changed in the future
 */
class GroupingStageInputsMerger(val testServices: TestServices, val workers: List<Worker>) {
    fun merge(nonGroupingStageOutputs: List<NonGroupingStageOutput>): GroupingStageInputArtifact {
        val secondStageConfiguration = CompilerConfiguration.create(messageCollector = MessageCollector.NONE)
        workers.forEach { worker ->
            worker.process(secondStageConfiguration, nonGroupingStageOutputs.map { it.testServices })
        }
        return GroupingStageInputArtifact(secondStageConfiguration, nonGroupingStageOutputs)
    }

    /**
     * Single unit of an artifact merging processing. Several workers could be registered in the test configuration.
     */
    abstract class Worker(val testServices: TestServices) {
        abstract fun process(configuration: CompilerConfiguration, firstStageServices: List<TestServices>)
    }
}

data class NonGroupingStageOutput(
    val testServices: TestServices,
    val catchingExecutor: CatchingExecutor,
) {
    val testInfo: KotlinTestInfo get() = testServices.testInfo

    /**
     * Allows executing code which potentially throws an exception during the grouping stage, so this exception
     * would be reported as a failure of the single test, not the whole group. The actual implementation is provided
     * by the test engine.
     */
    fun interface CatchingExecutor {
        fun executeWithCatching(exceptionWrapper: (Throwable) -> WrappedException, block: () -> Unit)
    }
}


class GroupingStageInputArtifact(
    val secondStageConfiguration: CompilerConfiguration,
    val nonGroupingStageOutputs: List<NonGroupingStageOutput>
) : ResultingArtifact<GroupingStageInputArtifact>() {
    object Kind : TestArtifactKind<GroupingStageInputArtifact>("SecondStageInputArtifact")

    override val kind: Kind get() = Kind
}

class GroupingStageInputsHolder(val nonGroupingStageOutputs: List<NonGroupingStageOutput>) : TestService

private val TestServices.groupingStageInputsHolder: GroupingStageInputsHolder by TestServices.testServiceAccessor()
val TestServices.groupingStageInputs: List<NonGroupingStageOutput>
    get() = groupingStageInputsHolder.nonGroupingStageOutputs
