/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.psi

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.analysis.decompiler.stub.files.AbstractDecompiledClassTest
import org.jetbrains.kotlin.idea.KotlinLanguage.INSTANCE
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.name
import kotlin.streams.toList

class BuiltinDecompilerToTextTest : AbstractDecompiledClassTest() {
    fun testBuiltinDecompilationToText() {
        val decompiledBuiltInKtFiles = loadBuiltIns()
        val testDataPath = Paths.get(TEST_DATA_PATH)
        for (file in decompiledBuiltInKtFiles) {
            val resultFileName = file.name + DECOMPILED_TEXT_EXTENSION
            val expectedFile = testDataPath.resolve(resultFileName)
            KotlinTestUtils.assertEqualsToFile(expectedFile, file.text)
        }
        val expectedBuiltins = Files.list(testDataPath).toList()
        assertEquals(
            expectedBuiltins.map { it.name.removeSuffix(DECOMPILED_TEXT_EXTENSION) }.sorted(),
            decompiledBuiltInKtFiles.map { it.name }.sorted(),
        )
    }

    private fun loadBuiltIns(): Collection<KtFile> {
        val psiManager = PsiManager.getInstance(project)
        val builtInDecompiler = KotlinBuiltInDecompiler()
        return BuiltInsVirtualFileProvider.getInstance().getBuiltInVirtualFiles().mapNotNull { virtualFile ->
            createKtFileStub(psiManager, builtInDecompiler, virtualFile)
        }
    }

    private fun createKtFileStub(
        psiManager: PsiManager,
        builtInDecompiler: KotlinBuiltInDecompiler,
        virtualFile: VirtualFile,
    ): KtFile? {
        val fileViewProvider = builtInDecompiler.createFileViewProvider(virtualFile, psiManager, physical = true)
        val psiFile = fileViewProvider.getPsi(INSTANCE)
        return psiFile as KtFile?
    }

    override fun setUp() {
        super.setUp()
        val applicationEnvironment = environment.projectEnvironment.environment
        val application = applicationEnvironment.application
        if (application.getService(BuiltInsVirtualFileProvider::class.java) == null) {
            application.registerService(
                BuiltInsVirtualFileProvider::class.java,
                BuiltInsVirtualFileProviderCliImpl(applicationEnvironment.jarFileSystem as CoreJarFileSystem)
            )
        }
    }

    companion object {
        private const val TEST_DATA_PATH = "analysis/decompiled/decompiler-to-psi/testData/builtins"
        private const val DECOMPILED_TEXT_EXTENSION = ".decompiled.text.kt"
    }
}