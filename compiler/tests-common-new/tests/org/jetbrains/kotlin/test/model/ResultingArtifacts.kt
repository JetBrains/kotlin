/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.model

import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.backend.wasm.WasmCompilerResult
import org.jetbrains.kotlin.codegen.ClassFileFactory
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fileClasses.JvmFileClassInfo
import org.jetbrains.kotlin.ir.backend.js.CompilerResult
import java.io.File

class SourceFileInfo(
    val sourceFile: KtSourceFile,
    val info: JvmFileClassInfo
)

object BinaryArtifacts {
    class Jvm(val classFileFactory: ClassFileFactory, val fileInfos: Collection<SourceFileInfo>) : ResultingArtifact.Binary<Jvm>() {
        override val kind: BinaryKind<Jvm>
            get() = ArtifactKinds.Jvm
    }

    class JvmFromK1AndK2(val fromK1: Jvm, val fromK2: Jvm) : ResultingArtifact.Binary<JvmFromK1AndK2>() {
        override val kind: BinaryKind<JvmFromK1AndK2>
            get() = ArtifactKinds.JvmFromK1AndK2
    }

    sealed class Js : ResultingArtifact.Binary<Js>() {
        abstract val outputFile: File
        override val kind: BinaryKind<Js>
            get() = ArtifactKinds.Js

        open fun unwrap(): Js = this

        class JsIrArtifact(override val outputFile: File, val compilerResult: CompilerResult, val icCache: Map<String, ByteArray>? = null) : Js()

        data class IncrementalJsArtifact(val originalArtifact: Js, val recompiledArtifact: Js) : Js() {
            override val outputFile: File
                get() = unwrap().outputFile

            override fun unwrap(): Js {
                return originalArtifact
            }
        }
    }

    class Native : ResultingArtifact.Binary<Native>() {
        override val kind: BinaryKind<Native>
            get() = ArtifactKinds.Native
    }

    class Wasm(
        val compilerResult: WasmCompilerResult,
        val compilerResultWithDCE: WasmCompilerResult,
        val compilerResultWithOptimizer: WasmCompilerResult?,
    ) : ResultingArtifact.Binary<Wasm>() {
        override val kind: BinaryKind<Wasm>
            get() = ArtifactKinds.Wasm
    }

    class KLib(val outputFile: File, val reporter: BaseDiagnosticsCollector) : ResultingArtifact.Binary<KLib>() {
        override val kind: BinaryKind<KLib>
            get() = ArtifactKinds.KLib
    }
}
