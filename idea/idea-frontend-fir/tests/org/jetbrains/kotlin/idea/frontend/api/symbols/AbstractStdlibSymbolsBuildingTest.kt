/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.symbols

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.idea.addExternalTestFiles
import org.jetbrains.kotlin.idea.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.analyze
import org.jetbrains.kotlin.idea.frontend.api.analyzeWithReadAction
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractStdlibSymbolsBuildingTest : KotlinLightCodeInsightFixtureTestCase() {
    protected fun doTest(path: String) {
        val fakeKtFile = myFixture.configureByText("file.kt", "fun a() {}") as KtFile

        val testDataFile = File(path)
        val testFileText = FileUtil.loadFile(testDataFile)

        val identifier = testFileText.substringBeforeLast(SYMBOLS_TAG).trim()

        val symbolData = SymbolData.create(identifier)

        val renderedSymbols = executeOnPooledThreadInReadAction {
            analyze(fakeKtFile) {
                val symbols = symbolData.toSymbols(this)
                symbols.map { DebugSymbolRenderer.render(it) }
            }
        }

        val actual = buildString {
            val actualSymbolsData = renderedSymbols.joinToString(separator = "\n")
            val fileTextWithoutSymbolsData = testFileText.substringBeforeLast(SYMBOLS_TAG).trimEnd()
            appendLine(fileTextWithoutSymbolsData)
            appendLine()
            appendLine(SYMBOLS_TAG)
            append(actualSymbolsData)
        }
        KotlinTestUtils.assertEqualsToFile(testDataFile, actual)
    }


    override fun getDefaultProjectDescriptor(): KotlinLightProjectDescriptor =
        KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    companion object {
        private const val SYMBOLS_TAG = "// SYMBOLS:"
    }
}

private sealed class SymbolData {
    abstract fun toSymbols(analysisSession: KtAnalysisSession): List<KtSymbol>

    data class ClassData(val classId: ClassId) : SymbolData() {
        override fun toSymbols(analysisSession: KtAnalysisSession): List<KtSymbol> {
            val symbol = analysisSession.symbolProvider.getClassOrObjectSymbolByClassId(classId) ?: error("Class $classId is not found")
            return listOf(symbol)
        }
    }

    data class CallableData(val packageName: FqName, val name: Name) : SymbolData() {
        override fun toSymbols(analysisSession: KtAnalysisSession): List<KtSymbol> {
            val symbols = analysisSession.symbolProvider.getTopLevelCallableSymbols(packageName, name).toList()
            if (symbols.isEmpty()) {
                error("No callable with fqName ${packageName}/${name} found")
            }
            return symbols
        }
    }

    companion object {
        fun create(data: String): SymbolData = when {
            data.startsWith("class:") -> ClassData(ClassId.fromString(data.removePrefix("class:").trim()))
            data.startsWith("callable:") -> {
                val fullName = data.removePrefix("callable:").trim()
                val name = Name.identifier(fullName.substringAfterLast("/"))
                val fqName = FqName(fullName.substringBeforeLast("/").replace('/', '.'))
                CallableData(fqName, name)
            }
            else -> error("Invalid symbol")
        }
    }
}
