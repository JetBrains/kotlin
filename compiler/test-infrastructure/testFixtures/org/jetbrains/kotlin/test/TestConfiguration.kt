/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.model.ResultingArtifact
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.MetaTestConfigurator
import org.jetbrains.kotlin.test.services.ModuleStructureExtractor
import org.jetbrains.kotlin.test.services.PreAnalysisHandler
import org.jetbrains.kotlin.test.services.TestServices

typealias Constructor<R> = (TestServices) -> R

typealias Constructor2<T, R> = (TestServices, T) -> R

interface TestConfiguration<Step : TestStep<*, *>> {
    val rootDisposable: Disposable
    val testServices: TestServices
    val directives: DirectivesContainer
    val defaultRegisteredDirectives: RegisteredDirectives
    val moduleStructureExtractor: ModuleStructureExtractor
    val preAnalysisHandlers: List<PreAnalysisHandler>
    val metaTestConfigurators: List<MetaTestConfigurator>
    val afterAnalysisCheckers: List<AfterAnalysisChecker>
    val metaInfoHandlerEnabled: Boolean

    val steps: List<Step>
}

interface NonGroupingPhaseTestConfiguration : TestConfiguration<TestStep.NonGroupingStep<*, *>> {
    var startingArtifactFactory: (TestModule) -> ResultingArtifact<*>
}

interface GroupingPhaseTestConfiguration : TestConfiguration<TestStep.GroupingPhaseStep<*, *>> {
    val mergerWorkers: List<GroupingPhaseInputsMerger.Worker>
}

// ---------------------------- Utils ----------------------------

fun <R> (() -> R).coerce(): Constructor<R> {
    return { this.invoke() }
}
