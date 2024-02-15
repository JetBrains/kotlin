/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.konan

import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import com.intellij.psi.PsiManager
import com.intellij.util.indexing.FileContentImpl
import org.jetbrains.kotlin.analysis.decompiler.stub.files.findMainTestKotlinFile
import org.jetbrains.kotlin.analysis.decompiler.stub.files.serializeToString
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.library.KLIB_METADATA_FILE_EXTENSION_WITH_DOT
import org.jetbrains.kotlin.psi.stubs.elements.KtFileStubBuilder
import org.jetbrains.kotlin.test.*
import org.jetbrains.kotlin.test.directives.model.Directive
import org.jetbrains.kotlin.test.directives.model.DirectiveApplicability
import org.jetbrains.kotlin.test.directives.model.SimpleDirective
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.utils.addIfNotNull
import org.junit.Assert
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors
import kotlin.io.path.absolutePathString
import kotlin.io.path.extension

abstract class AbstractDecompiledKnmStubConsistencyFe10Test : AbstractDecompiledKnmStubConsistencyTest() {
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
}

abstract class AbstractDecompiledKnmStubConsistencyK2Test : AbstractDecompiledKnmStubConsistencyTest() {
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
}

abstract class AbstractDecompiledKnmStubConsistencyTest : AbstractDecompiledKnmFileTest() {
    abstract fun createDecompiler(): KlibMetadataDecompiler<*>

    override fun doTest(testDirectoryPath: Path) {
        val files = compileToKnmFiles(testDirectoryPath)

        for (knmFile in files) {
            checkKnmStubConsistency(knmFile)
        }
    }

    private fun checkKnmStubConsistency(knmFile: VirtualFile) {
        val stubTreeBinaryFile = createDecompiler().stubBuilder.buildFileStub(FileContentImpl.createByFile(knmFile, environment.project))!!

        val fileViewProviderForDecompiledFile = K2KotlinNativeMetadataDecompiler().createFileViewProvider(
            knmFile, PsiManager.getInstance(project), physical = false,
        )

        val stubTreeForDecompiledFile = KtFileStubBuilder().buildStubTree(
            KlibDecompiledFile(fileViewProviderForDecompiledFile) { virtualFile ->
                createDecompiler().buildDecompiledTextForTests(virtualFile)
            }
        )

        Assert.assertEquals(
            "PSI and deserialized stubs don't match",
            stubTreeForDecompiledFile.serializeToString(),
            stubTreeBinaryFile.serializeToString()
        )
    }
}
