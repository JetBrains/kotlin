/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.konan

import org.jetbrains.kotlin.cli.metadata.K2MetadataCompiler
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.test.CompilerTestUtil
import org.jetbrains.kotlin.test.KlibTestUtil
import org.jetbrains.kotlin.test.directives.model.DirectiveApplicability
import org.jetbrains.kotlin.test.directives.model.SimpleDirective
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import java.nio.file.Path
import kotlin.io.path.absolutePathString

interface KnmTestSupport {
    val ignoreDirective: SimpleDirective
    fun createDecompiler(): KlibMetadataDecompiler<*>
    fun compileCommonMetadata(inputKtFiles: List<Path>, compilationOutputPath: Path, additionalArguments: List<String>): OutputType
}

object Fe10KnmTestSupport : KnmTestSupport {
    private object Directives : SimpleDirectivesContainer() {
        val KNM_FE10_IGNORE by directive(
            description = "Ignore test for KNM files with FE10 K/N Decompiler",
            applicability = DirectiveApplicability.Global,
        )
    }

    override val ignoreDirective: SimpleDirective
        get() = Directives.KNM_FE10_IGNORE

    override fun createDecompiler(): KlibMetadataDecompiler<*> {
        return KotlinNativeMetadataDecompiler()
    }

    override fun compileCommonMetadata(
        inputKtFiles: List<Path>,
        compilationOutputPath: Path,
        additionalArguments: List<String>,
    ): OutputType {
        KlibTestUtil.compileCommonSourcesToKlib(
            inputKtFiles.map(Path::toFile),
            libraryName = "library",
            compilationOutputPath.toFile(),
            additionalArguments,
        )

        return OutputType.KLIB
    }
}

object K2KnmTestSupport : KnmTestSupport {
    private object Directives : SimpleDirectivesContainer() {
        val KNM_K2_IGNORE by directive(
            description = "Ignore test for KNM files with K2 K/N Decompiler",
            applicability = DirectiveApplicability.Global,
        )
    }

    override val ignoreDirective: SimpleDirective
        get() = Directives.KNM_K2_IGNORE

    override fun createDecompiler(): KlibMetadataDecompiler<*> {
        return K2KotlinNativeMetadataDecompiler()
    }

    override fun compileCommonMetadata(
        inputKtFiles: List<Path>,
        compilationOutputPath: Path,
        additionalArguments: List<String>,
    ): OutputType {
        CompilerTestUtil.executeCompilerAssertSuccessful(
            K2MetadataCompiler(), buildList {
                addAll(inputKtFiles.map { it.absolutePathString() })
                add("-d"); add(compilationOutputPath.absolutePathString())
                add("-module-name"); add("library")
                add("-classpath"); add(ForTestCompileRuntime.stdlibCommonForTests().absolutePath)
                addAll(additionalArguments)
            }
        )

        return OutputType.UNPACKED
    }
}

enum class OutputType {
    KLIB, UNPACKED
}
