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

typealias Constructor3<T1, T2, R> = (TestServices, T1, T2) -> R

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

fun <T, R> Constructor2<T, R>.bind(value: T): Constructor<R> {
    return { this.invoke(it, value) }
}

fun <T1, T2, R> Constructor3<T1, T2, R>.bind(value1: T1, value2: T2): Constructor<R> {
    return { this.invoke(it, value1, value2) }
}

fun <R> (() -> R).coerce(): Constructor<R> {
    return { this.invoke() }
}
