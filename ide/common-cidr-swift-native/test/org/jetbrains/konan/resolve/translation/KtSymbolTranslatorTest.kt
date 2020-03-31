package org.jetbrains.konan.resolve.translation

import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.testFramework.LightProjectDescriptor
import com.jetbrains.cidr.lang.CLanguageKind
import com.jetbrains.cidr.lang.preprocessor.OCInclusionContext
import com.jetbrains.cidr.lang.symbols.symtable.FileSymbolTablesCache
import com.jetbrains.cidr.lang.symbols.symtable.FileSymbolTablesCache.SymbolsProperties
import com.jetbrains.cidr.lang.symbols.symtable.FileSymbolTablesCache.SymbolsProperties.SymbolsKind
import org.jetbrains.konan.resolve.konan.KonanBridgeFileManager
import org.jetbrains.konan.resolve.konan.KonanBridgePsiFile
import org.jetbrains.konan.resolve.konan.KonanTarget
import org.jetbrains.konan.resolve.symbols.objc.KtOCInterfaceSymbol
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.JUnit3WithIdeaConfigurationRunner
import org.jetbrains.kotlin.test.testFramework.runWriteAction
import org.junit.Assume.assumeFalse
import org.junit.runner.RunWith

@RunWith(JUnit3WithIdeaConfigurationRunner::class)
class KtSymbolTranslatorTest : KotlinLightCodeInsightFixtureTestCase() {
    private val translator: KtOCSymbolTranslator
        get() = KtOCSymbolTranslator(project)

    override fun getProjectDescriptor(): LightProjectDescriptor = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    private object TestTarget : KonanTarget {
        override val moduleId: String
            get() = ":module"
        override val productModuleName: String
            get() = "MyModule"

        override fun equals(other: Any?): Boolean = super.equals(other)
        override fun hashCode(): Int = super.hashCode()
    }

    override fun setUp(): Unit = super.setUp().also {
        FileSymbolTablesCache.setShouldBuildTablesInTests(SymbolsProperties(SymbolsKind.ONLY_USED, false, false))
        FileSymbolTablesCache.forceSymbolsLoadedInTests(true)
    }

    override fun tearDown() {
        try {
            FileSymbolTablesCache.forceSymbolsLoadedInTests(null)
            FileSymbolTablesCache.setShouldBuildTablesInTests(null)
        } catch (e: Throwable) {
            addSuppressedException(e)
        } finally {
            super.tearDown()
        }
    }

    fun `test simple class translation`() {
        val file = configure("class A")
        val translatedSymbol = translator.translate(file, TestTarget).single() as KtOCInterfaceSymbol
        assertEquals("MyModuleA", translatedSymbol.name)
        assertFalse("state already loaded", translatedSymbol.stateLoaded)
        assertFalse(translatedSymbol.isTemplateSymbol)
        assertEquals("MyModuleBase", translatedSymbol.superType.name)
        assertTrue("state not loaded", translatedSymbol.stateLoaded)
    }

    fun `test stop translating after invalidation`() {
        val file = configure("class A")
        val translatedSymbol = translator.translate(file, TestTarget).single() as KtOCInterfaceSymbol
        assertFalse("state already loaded", translatedSymbol.stateLoaded)

        runWriteAction {
            val document = myFixture.getDocument(file)
            document.setText("class B")
            PsiDocumentManager.getInstance(project).commitDocument(document)
        }

        assertEquals("MyModuleA", translatedSymbol.name)
        assertEquals("", translatedSymbol.superType.name)
        assertFalse("state not loaded", translatedSymbol.stateLoaded)
    }

    fun `test stop translating after invalidation by adjacent file`() {
        val file = configure("class A")
        val translatedSymbol = translator.translate(file, TestTarget).single() as KtOCInterfaceSymbol
        assertFalse("state already loaded", translatedSymbol.stateLoaded)

        configure("typealias B = Unit", fileName = "other")

        assertEquals("MyModuleA", translatedSymbol.name)
        assertEquals("", translatedSymbol.superType.name)
        assertFalse("state not loaded", translatedSymbol.stateLoaded)
    }

    fun `test file symbol table invalidation`() {
        val file = configure("class A")
        val virtualFile = file.virtualFile

        val cache = FileSymbolTablesCache.getInstance(project)
        assumeFalse("cache already contains file", cache.cachedFiles.contains(virtualFile))

        val bridgingFile = KonanBridgeFileManager.getInstance(project).forTarget(TestTarget, "testTarget.h")
        val psiBridgingFile = PsiManager.getInstance(project).findFile(bridgingFile) as KonanBridgePsiFile
        val context = OCInclusionContext.empty(CLanguageKind.OBJ_C, psiBridgingFile).apply { addProcessedFile(bridgingFile) }

        val table = cache.forFile(virtualFile, context)
        assertNotNull("table could not be built", table)
        assertTrue("table not valid", table!!.isValid)

        runWriteAction {
            val document = myFixture.getDocument(file)
            document.setText("class B")
            PsiDocumentManager.getInstance(project).commitDocument(document)
        }

        assertFalse("table still valid", table.isValid)
    }

    fun `test file symbol table not invalidate by in-code-block change`() {
        val file = configure("fun a() { <caret> }")
        val virtualFile = file.virtualFile

        val cache = FileSymbolTablesCache.getInstance(project)
        assumeFalse("cache already contains file", cache.cachedFiles.contains(virtualFile))

        val bridgingFile = KonanBridgeFileManager.getInstance(project).forTarget(TestTarget, "testTarget.h")
        val psiBridgingFile = PsiManager.getInstance(project).findFile(bridgingFile) as KonanBridgePsiFile
        val context = OCInclusionContext.empty(CLanguageKind.OBJ_C, psiBridgingFile).apply { addProcessedFile(bridgingFile) }

        val table = cache.forFile(virtualFile, context)
        assertNotNull("table could not be built", table)
        assertTrue("table not valid", table!!.isValid)

        runWriteAction {
            val document = myFixture.getDocument(file)
            document.setText("fun a() { print(\"Hello\") }")
            PsiDocumentManager.getInstance(project).commitDocument(document)
        }

        assertTrue("table no longer valid", table.isValid)
    }

    private fun configure(code: String, fileName: String = "toTranslate"): KtFile =
        myFixture.configureByText("$fileName.kt", code) as KtFile
}