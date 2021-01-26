/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.model

import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.services.ServiceRegistrationData
import org.jetbrains.kotlin.test.services.TestServices

abstract class AbstractTestFacade<I : ResultingArtifact<I>, O : ResultingArtifact<O>> {
    abstract val inputKind: TestArtifactKind<I>
    abstract val outputKind: TestArtifactKind<O>

    abstract fun transform(module: TestModule, inputArtifact: I): O?

    open val additionalServices: List<ServiceRegistrationData>
        get() = emptyList()

    open val additionalDirectives: List<DirectivesContainer>
        get() = emptyList()
}

abstract class FrontendFacade<R : ResultingArtifact.FrontendOutput<R>>(
    val testServices: TestServices,
    final override val outputKind: FrontendKind<R>
) : AbstractTestFacade<ResultingArtifact.Source, R>() {
    final override val inputKind: TestArtifactKind<ResultingArtifact.Source>
        get() = SourcesKind

    abstract fun analyze(module: TestModule): R

    final override fun transform(module: TestModule, inputArtifact: ResultingArtifact.Source): R {
        // TODO: pass sources
        return analyze(module)
    }
}

abstract class Frontend2BackendConverter<R : ResultingArtifact.FrontendOutput<R>, I : ResultingArtifact.BackendInput<I>>(
    val testServices: TestServices,
    final override val inputKind: FrontendKind<R>,
    final override val outputKind: BackendKind<I>
) : AbstractTestFacade<R, I>()

abstract class BackendFacade<I : ResultingArtifact.BackendInput<I>, A : ResultingArtifact.Binary<A>>(
    val testServices: TestServices,
    final override val inputKind: BackendKind<I>,
    final override val outputKind: BinaryKind<A>
) : AbstractTestFacade<I, A>()
