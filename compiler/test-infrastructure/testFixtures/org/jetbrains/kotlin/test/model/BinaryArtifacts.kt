/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.model

import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.utils.fileUtils.withReplacedExtensionOrNull
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

        open val dtsFile: File?
            get() = outputFile.withReplacedExtensionOrNull("_v5.js", ".d.ts")
                ?: outputFile.withReplacedExtensionOrNull("_v5.mjs", ".d.ts")
    }

    class Native(val executable: File) : ResultingArtifact.Binary<Native>() {
        override val kind: ArtifactKind<Native>
            get() = ArtifactKinds.Native
    }

    abstract class Wasm: ResultingArtifact.Binary<Wasm>() {
        override val kind: ArtifactKind<Wasm>
            get() = ArtifactKinds.Wasm
    }

    class KLib(val outputFile: File, val reporter: BaseDiagnosticsCollector) : ResultingArtifact.Binary<KLib>() {
        override val kind: ArtifactKind<KLib>
            get() = ArtifactKinds.KLib
    }
}
