/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaPlatformInterface
import org.jetbrains.kotlin.analysis.api.impl.base.projectStructure.KaBuiltinsModuleImpl
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirBinaryTestConfigurator
import org.jetbrains.kotlin.analysis.stubs.CompiledStubsTestEngine
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModuleFactory
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.TestModuleKind
import org.jetbrains.kotlin.metadata.builtins.BuiltInsBinaryVersion
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.targetPlatform
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.name

class BuiltinsDecompilerTest : AbstractAnalysisApiExecutionTest("testData/builtins/customData") {
    override val configurator: AnalysisApiTestConfigurator = object : AnalysisApiFirBinaryTestConfigurator() {
        override val testModuleFactory: KtTestModuleFactory = object : KtTestModuleFactory {
            override fun createModule(
                testModule: TestModule,
                contextModule: KtTestModule?,
                dependencyBinaryRoots: Collection<Path>,
                testServices: TestServices,
                project: Project,
            ): KtTestModule {
                val path = testModule.files.single().originalFile.path
                val virtualFile = VirtualFileManager.getInstance().findFileByUrl("file://$path")
                    ?: error("VirtualFile not found for $path")

                val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
                    ?: error("PsiFile not found for $virtualFile")

                @OptIn(KaImplementationDetail::class, KaPlatformInterface::class)
                return KtTestModule(
                    TestModuleKind.LibraryBinary,
                    testModule,
                    KaBuiltinsModuleImpl(testModule.targetPlatform(testServices), project),
                    listOf(psiFile),
                )
            }
        }
    }

    @Test
    fun wrong(file: KtFile, testServices: TestServices) {
        testServices.assertions.assertEquals(testDataPath.name, file.name)

        val expectedDecompiledText = """
            // This file was compiled with a newer version of Kotlin compiler and can't be decompiled.
            //
            // The current compiler supports reading only metadata of version ${BuiltInsBinaryVersion.INSTANCE} or lower.
            // The file metadata version is 0.42.239
        """.trimIndent()

        testServices.assertions.assertEquals(expectedDecompiledText, file.text)

        val expectedInvalidStub = """
                |FILE[kind=Invalid[errorMessage=$expectedDecompiledText]]
                |  PACKAGE_DIRECTIVE
                |  IMPORT_LIST

            """.trimMargin()

        val fileStub = CompiledStubsTestEngine.compute(file)

        testServices.assertions.assertEquals(
            expected = expectedInvalidStub,
            actual = CompiledStubsTestEngine.render(fileStub),
        )

        CompiledStubsTestEngine.validate(testServices, file, fileStub)
    }
}
