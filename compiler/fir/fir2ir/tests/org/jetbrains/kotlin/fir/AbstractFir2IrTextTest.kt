/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.fir.analysis.FirAnalyzerFacade
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

    override fun doTest(filePath: String) {
        val oldFrontendTextPath = filePath.replace(".kt", ".txt")
        val firTextPath = filePath.replace(".kt", ".fir.txt")
        val oldFrontendTextFile = File(oldFrontendTextPath)
        val firTextFile = File(firTextPath)
        if (oldFrontendTextFile.exists() && firTextFile.exists()) {
            compareAndMergeFirFileAndOldFrontendFile(oldFrontendTextFile, firTextFile, compareWithTrimming = true)
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
        val firAnalyzerFacade = FirAnalyzerFacade(session, myEnvironment.configuration.languageVersionSettings, psiFiles)
        return firAnalyzerFacade.convertToIr(generateFacades = false).irModuleFragment
    }
}
