/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.model

abstract class TestArtifactKind<R : ResultingArtifact<R>>(private val representation: String) {
    open val shouldRunAnalysis: Boolean
        get() = true

    override fun toString(): String {
        return representation
    }
}

object SourcesKind : TestArtifactKind<ResultingArtifact.Source>("Sources")

abstract class FrontendKind<R : ResultingArtifact.FrontendOutput<R>>(representation: String) : TestArtifactKind<R>(representation) {
    object NoFrontend : FrontendKind<ResultingArtifact.FrontendOutput.Empty>("NoFrontend") {
        override val shouldRunAnalysis: Boolean
            get() = false
    }
}

abstract class BackendKind<I : ResultingArtifact.BackendInput<I>>(representation: String) : TestArtifactKind<I>(representation) {
    object NoBackend : BackendKind<ResultingArtifact.BackendInput.Empty>("NoBackend") {
        override val shouldRunAnalysis: Boolean
            get() = false
    }
}

abstract class BinaryKind<A : ResultingArtifact.Binary<A>>(representation: String) : TestArtifactKind<A>(representation) {
    object NoArtifact : BinaryKind<ResultingArtifact.Binary.Empty>("NoArtifact") {
        override val shouldRunAnalysis: Boolean
            get() = false
    }
}
