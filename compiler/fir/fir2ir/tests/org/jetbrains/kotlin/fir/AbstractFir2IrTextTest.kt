/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.search.GlobalSearchScope
import junit.framework.TestCase
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.fir.backend.Fir2IrConverter
import org.jetbrains.kotlin.fir.builder.RawFirBuilder
import org.jetbrains.kotlin.fir.resolve.firProvider
import org.jetbrains.kotlin.fir.resolve.impl.FirProviderImpl
import org.jetbrains.kotlin.fir.resolve.transformers.FirTotalResolveTransformer
import org.jetbrains.kotlin.ir.AbstractIrTextTestCase
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import java.io.File

abstract class AbstractFir2IrTextTest : AbstractIrTextTestCase() {

    private fun prepareProjectExtensions(project: Project) {
        Extensions.getArea(project)
            .getExtensionPoint(PsiElementFinder.EP_NAME)
            .unregisterExtension(JavaElementFinder::class.java)
    }

    override fun doTest(wholeFile: File, testFiles: List<TestFile>) {
        buildFragmentAndTestIt(wholeFile, testFiles)
    }

    override fun doTest(filePath: String?) {
        if (filePath != null) {
            val originalTextPath = filePath.replace(".kt", ".txt")
            val firTextPath = filePath.replace(".kt", ".fir.txt")
            val originalText = File(originalTextPath)
            val firText = File(firTextPath)
            if (originalText.exists() && firText.exists()) {
                val originalLines = originalText.readLines()
                val firLines = firText.readLines()
                TestCase.assertFalse(
                    "Dumps via FIR & via old FE are the same. Please delete .fir.txt dump and add // FIR_IDENTICAL to test source",
                    firLines.withIndex().all { (index, line) ->
                        val trimmed = line.trim()
                        val originalTrimmed = originalLines.getOrNull(index)?.trim()
                        trimmed.isEmpty() && originalTrimmed?.isEmpty() != false || trimmed == originalTrimmed
                    } && originalLines.withIndex().all { (index, line) ->
                        index < firLines.size || line.trim().isEmpty()
                    }
                )
            }
        }
        super.doTest(filePath)
    }

    override fun getExpectedTextFileName(testFile: TestFile, name: String): String {
        // NB: replace with if (true) to make test against old FE results
        if ("// FIR_IDENTICAL" in testFile.content.split("\n")) {
            return super.getExpectedTextFileName(testFile, name)
        }
        return name.replace(".txt", ".fir.txt").replace(".kt", ".fir.txt")
    }

    override fun generateIrModule(ignoreErrors: Boolean): IrModuleFragment {
        val psiFiles = myFiles.psiFiles

        val project = psiFiles.first().project
        prepareProjectExtensions(project)

        val scope = GlobalSearchScope.filesScope(project, psiFiles.map { it.virtualFile })
            .uniteWith(TopDownAnalyzerFacadeForJVM.AllJavaSourcesInProjectScope(project))
        val session = createSession(myEnvironment, scope)

        val firProvider = (session.firProvider as FirProviderImpl)
        val builder = RawFirBuilder(session, firProvider.kotlinScopeProvider, stubMode = false)

        val resolveTransformer = FirTotalResolveTransformer()
        val firFiles = psiFiles.map {
            val firFile = builder.buildFirFile(it)
            firProvider.recordFile(firFile)
            firFile
        }.also {
            try {
                resolveTransformer.processFiles(it)
            } catch (e: Exception) {
                throw e
            }
        }

        return Fir2IrConverter.createModuleFragment(
            session, firFiles, myEnvironment.configuration.languageVersionSettings
        ).irModuleFragment
    }
}