/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.psi

import com.intellij.openapi.extensions.LoadingOrder
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.util.indexing.FileContentImpl
import com.intellij.psi.compiled.ClassFileDecompilers
import com.intellij.psi.impl.compiled.ClassFileStubBuilder
import com.intellij.psi.stubs.BinaryFileStubBuilders
import org.jetbrains.kotlin.analysis.decompiler.stub.files.AbstractDecompiledClassTest
import org.jetbrains.kotlin.analysis.decompiler.stub.files.serializeToString
import org.jetbrains.kotlin.idea.KotlinLanguage.INSTANCE
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import kotlin.io.path.name
import kotlin.streams.asSequence

class BuiltinDecompilerToTextTest : AbstractDecompiledClassTest() {
    fun testBuiltinDecompilationToText() {
        val decompiledBuiltInKtFiles = loadBuiltIns()
        val testDataPath = Paths.get(TEST_DATA_PATH)
        for (file in decompiledBuiltInKtFiles) {
            val decompiledTextFileName = file.name + DECOMPILED_TEXT_EXTENSION
            val decompiledTextExpectedFile = testDataPath.resolve(decompiledTextFileName)
            KotlinTestUtils.assertEqualsToFile(decompiledTextExpectedFile, file.text)

            val decompiledStubFileName = file.name + DECOMPILED_STUB_EXTENSION
            val decompiledStubExpectedFile = testDataPath.resolve(decompiledStubFileName)
            val decompiledStubString = file.calcStubTree().root.serializeToString()
            KotlinTestUtils.assertEqualsToFile(decompiledStubExpectedFile, decompiledStubString)

            val compiledStubFileName = file.name + COMPILED_STUB_EXTENSION
            val compiledStubExpectedFile = testDataPath.resolve(compiledStubFileName)
            val compiledStubString = KotlinBuiltInDecompiler().stubBuilder
                .buildFileStub(FileContentImpl.createByFile(file.virtualFile))!!
                .serializeToString()

            KotlinTestUtils.assertEqualsToFile(compiledStubExpectedFile, compiledStubString)
        }

        val expectedBuiltins = Files.list(testDataPath).asSequence().mapTo(TreeSet()) {
            it.name.substringBefore(KotlinBuiltInFileType.defaultExtension) + KotlinBuiltInFileType.defaultExtension
        }

        assertEquals(
            expectedBuiltins.sorted(),
            decompiledBuiltInKtFiles.map { it.name }.sorted(),
        )
    }

    private fun loadBuiltIns(): Collection<KtFile> {
        val psiManager = PsiManager.getInstance(project)
        val builtInDecompiler = KotlinBuiltInDecompiler()
        return BuiltinsVirtualFileProvider.getInstance().getBuiltinVirtualFiles().mapNotNull { virtualFile ->
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
        if (application.getService(BuiltinsVirtualFileProvider::class.java) == null) {
            application.registerService(
                BuiltinsVirtualFileProvider::class.java,
                BuiltinsVirtualFileProviderCliImpl()
            )

            applicationEnvironment.registerFileType(KotlinBuiltInFileType, BuiltInSerializerProtocol.BUILTINS_FILE_EXTENSION)
            BinaryFileStubBuilders.INSTANCE.addExplicitExtension(KotlinBuiltInFileType, ClassFileStubBuilder())

            ClassFileDecompilers.getInstance().EP_NAME.point.registerExtension(
                KotlinBuiltInDecompiler(),
                LoadingOrder.FIRST,
                this.testRootDisposable
            )
        }
    }

    companion object {
        private const val TEST_DATA_PATH = "analysis/decompiled/decompiler-to-psi/testData/builtins"
        private const val DECOMPILED_TEXT_EXTENSION = ".decompiled.text.kt"
        private const val DECOMPILED_STUB_EXTENSION = ".decompiled.stubs.txt"
        private const val COMPILED_STUB_EXTENSION = ".compiled.stubs.txt"
    }
}