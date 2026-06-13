/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.model

import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.backend.wasm.WasmCompilerResult
import org.jetbrains.kotlin.codegen.ClassFileFactory
import org.jetbrains.kotlin.fileClasses.JvmFileClassInfo
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.CompilerResult
import org.jetbrains.kotlin.wasm.ir.WasmModule
import java.io.File

class SourceFileInfo(
    val sourceFile: KtSourceFile,
    val info: JvmFileClassInfo
)

class JvmClassFileArtifact(val classFileFactory: ClassFileFactory, val fileInfos: Collection<SourceFileInfo>) : BinaryArtifacts.Jvm()

class JsIrArtifact(override val outputFile: File, val compilerResult: CompilerResult, val icCache: Map<String, ByteArray>? = null) : BinaryArtifacts.Js()

class JsTypeScriptArtifact(override val outputFile: File) : BinaryArtifacts.Js()

data class IncrementalJsArtifact(val originalArtifact: BinaryArtifacts.Js, val recompiledArtifact: BinaryArtifacts.Js) : BinaryArtifacts.Js() {
    override val outputFile: File
        get() = unwrap().outputFile

    override fun unwrap(): BinaryArtifacts.Js {
        return originalArtifact
    }
}

class WasmCompilationSet(
    val compiledModule: WasmModule,
    val compilerResult: WasmCompilerResult,
    val compilationDependencies: List<WasmCompilationSet> = emptyList(),
)

class WasmCompilationSetsBinaryArtifact(
    val compilation: WasmCompilationSet,
    val dceCompilation: WasmCompilationSet? = null,
    val optimisedCompilation: WasmCompilationSet? = null,
) : BinaryArtifacts.Wasm()

class WasmFolderBinaryArtifact(val folder: File) : BinaryArtifacts.Wasm()
