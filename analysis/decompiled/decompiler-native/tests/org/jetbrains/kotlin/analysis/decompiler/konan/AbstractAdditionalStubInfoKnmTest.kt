/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.konan

import com.intellij.util.indexing.FileContentImpl
import org.jetbrains.kotlin.analysis.decompiler.stub.files.extractAdditionalStubInfo
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.directives.model.Directive
import org.jetbrains.kotlin.test.directives.model.DirectiveApplicability
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import java.nio.file.Path
import kotlin.io.path.name

abstract class AbstractAdditionalStubInfoKnmTest : AbstractDecompiledKnmFileTest() {
    private object Directives : SimpleDirectivesContainer() {
        val KNM_K2_IGNORE by directive(
            description = "Ignore test for KNM files with K2 K/N Decompiler",
            applicability = DirectiveApplicability.Global,
        )
    }

    override val ignoreDirective: Directive
        get() = Directives.KNM_K2_IGNORE

    override fun doTest(testDirectoryPath: Path) {
        val stubBuilder = K2KotlinNativeMetadataDecompiler().stubBuilder
        val knmFiles = compileToKnmFiles(testDirectoryPath)
        val knmFile = knmFiles.singleOrNull { "root_package" !in it.path }
            ?: error("Expected a single non-root .knm file, but received:${System.lineSeparator()}" +
                             knmFiles.joinToString(separator = System.lineSeparator()) { it.path }
            )

        val stub = stubBuilder.buildFileStub(FileContentImpl.createByFile(knmFile, environment.project))!!
        KotlinTestUtils.assertEqualsToFile(
            testDirectoryPath.resolve("${testDirectoryPath.name}.txt"),
            extractAdditionalStubInfo(stub)
        )
    }
}
