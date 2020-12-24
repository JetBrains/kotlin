/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.MetaTestConfigurator
import org.jetbrains.kotlin.test.services.ModuleStructureExtractor
import org.jetbrains.kotlin.test.services.TestServices

typealias Constructor<T> = (TestServices) -> T

abstract class TestConfiguration {
    abstract val rootDisposable: Disposable

    abstract val testServices: TestServices

    abstract val directives: DirectivesContainer

    abstract val defaultRegisteredDirectives: RegisteredDirectives

    abstract val moduleStructureExtractor: ModuleStructureExtractor

    abstract val metaTestConfigurators: List<MetaTestConfigurator>

    abstract val afterAnalysisCheckers: List<AfterAnalysisChecker>

    abstract val metaInfoHandlerEnabled: Boolean

    abstract fun <I : ResultingArtifact<I>, O : ResultingArtifact<O>> getFacade(
        inputKind: TestArtifactKind<I>,
        outputKind: TestArtifactKind<O>
    ): AbstractTestFacade<I, O>

    abstract fun <A : ResultingArtifact<A>> getHandlers(artifactKind: TestArtifactKind<A>): List<AnalysisHandler<A>>

    abstract fun getAllHandlers(): List<AnalysisHandler<*>>
}

