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

typealias Constructor<T> = (TestServices) -> T

abstract class TestConfiguration {
    abstract val rootDisposable: Disposable

    abstract val testServices: TestServices

    abstract val directives: DirectivesContainer

    abstract val defaultRegisteredDirectives: RegisteredDirectives

    abstract val moduleStructureExtractor: ModuleStructureExtractor

    abstract val preAnalysisHandlers: List<PreAnalysisHandler>

    abstract val metaTestConfigurators: List<MetaTestConfigurator>

    abstract val afterAnalysisCheckers: List<AfterAnalysisChecker>

    abstract val startingArtifactFactory: (TestModule) -> ResultingArtifact<*>

    abstract val steps: List<TestStep<*, *>>

    abstract val metaInfoHandlerEnabled: Boolean
}

// ---------------------------- Utils ----------------------------

fun <T, R> ((TestServices, T) -> R).bind(value: T): Constructor<R> {
    return { this.invoke(it, value) }
}
