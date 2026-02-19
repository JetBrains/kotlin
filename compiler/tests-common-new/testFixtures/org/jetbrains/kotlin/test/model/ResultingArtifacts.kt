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
import org.jetbrains.kotlin.wasm.ir.WasmModule
import org.jetbrains.kotlin.utils.fileUtils.withReplacedExtensionOrNull
import java.io.File

class SourceFileInfo(
    val sourceFile: KtSourceFile,
    val info: JvmFileClassInfo
)

object BinaryArtifacts {
    class Jvm(val classFileFactory: ClassFileFactory, val fileInfos: Collection<SourceFileInfo>) : ResultingArtifact.Binary<Jvm>() {
        override val kind: ArtifactKind<Jvm>
            get() = ArtifactKinds.Jvm
    }

    sealed class Js : ResultingArtifact.Binary<Js>() {
        abstract val outputFile: File
        override val kind: ArtifactKind<Js>
            get() = ArtifactKinds.Js

        open fun unwrap(): Js = this

        open val dtsFile: File?
            get() = outputFile.withReplacedExtensionOrNull("_v5.js", ".d.ts")
                ?: outputFile.withReplacedExtensionOrNull("_v5.mjs", ".d.ts")

        class JsIrArtifact(override val outputFile: File, val compilerResult: CompilerResult, val icCache: Map<String, ByteArray>? = null) : Js()

        class TypeScriptArtifact(override val outputFile: File) : Js() {
            override val dtsFile: File
                get() = outputFile
        }

        data class IncrementalJsArtifact(val originalArtifact: Js, val recompiledArtifact: Js) : Js() {
            override val outputFile: File
                get() = unwrap().outputFile

            override fun unwrap(): Js {
                return originalArtifact
            }
        }
    }

    class Native(val executable: File) : ResultingArtifact.Binary<Native>() {
        override val kind: ArtifactKind<Native>
            get() = ArtifactKinds.Native
    }

    class WasmCompilationSet(
        val compiledModule: WasmModule,
        val compilerResult: WasmCompilerResult,
        val compilationDependencies: List<WasmCompilationSet> = emptyList(),
    )

    sealed class Wasm: ResultingArtifact.Binary<Wasm>() {
        override val kind: ArtifactKind<Wasm>
            get() = ArtifactKinds.Wasm

        class CompilationSets(
            val compilation: WasmCompilationSet,
            val dceCompilation: WasmCompilationSet? = null,
            val optimisedCompilation: WasmCompilationSet? = null,
        ) : Wasm()

        class Folder(val folder: File) : Wasm()
    }

    class KLib(val outputFile: File, val reporter: BaseDiagnosticsCollector) : ResultingArtifact.Binary<KLib>() {
        override val kind: ArtifactKind<KLib>
            get() = ArtifactKinds.KLib
    }
}
