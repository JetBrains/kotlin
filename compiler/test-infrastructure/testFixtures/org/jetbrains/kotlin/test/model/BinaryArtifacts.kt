/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.model

import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import java.io.File

object BinaryArtifacts {
    abstract class Jvm : ResultingArtifact.Binary<Jvm>() {
        override val kind: ArtifactKind<Jvm>
            get() = ArtifactKinds.Jvm
    }

    abstract class Js : ResultingArtifact.Binary<Js>() {
        override val kind: ArtifactKind<Js>
            get() = ArtifactKinds.Js

        abstract val outputFile: File

        open fun unwrap(): Js = this
    }

    class Native(val executable: File) : ResultingArtifact.Binary<Native>() {
        override val kind: ArtifactKind<Native>
            get() = ArtifactKinds.Native
    }

    abstract class Wasm: ResultingArtifact.Binary<Wasm>() {
        override val kind: ArtifactKind<Wasm>
            get() = ArtifactKinds.Wasm

        /**
         * `true` when this executable was linked together with the grouped tests' result-collecting driver, so its VM
         * output has to carry a structured result block. Whether a batch gets the driver is decided per pipeline by the
         * grouping-stage facade that produced this artifact, so the batch size does not answer it: a test that merely
         * ended up alone in its batch is driver-driven too.
         */
        open val hasGroupedTestsDriver: Boolean
            get() = false
    }

    class KLib(val outputFile: File, val reporter: BaseDiagnosticsCollector) : ResultingArtifact.Binary<KLib>() {
        override val kind: ArtifactKind<KLib>
            get() = ArtifactKinds.KLib
    }
}
