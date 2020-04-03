package org.jetbrains.konan.resolve.translation

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.PlatformTestUtil
import com.jetbrains.cidr.lang.CLanguageKind
import com.jetbrains.cidr.lang.preprocessor.OCInclusionContext
import com.jetbrains.cidr.lang.symbols.symtable.FileSymbolTablesCache
import com.jetbrains.cidr.lang.symbols.symtable.FileSymbolTablesCache.SymbolsProperties
import com.jetbrains.cidr.lang.symbols.symtable.FileSymbolTablesCache.SymbolsProperties.SymbolsKind
import org.jetbrains.konan.resolve.konan.KonanBridgeFileManager
import org.jetbrains.konan.resolve.konan.KonanBridgePsiFile
import org.jetbrains.konan.resolve.konan.KonanTarget
import org.jetbrains.konan.resolve.symbols.objc.KtOCInterfaceSymbol
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
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

    private val cache: FileSymbolTablesCache
        get() = FileSymbolTablesCache.getInstance(project)

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
        assertOCInterfaceSymbol(translatedSymbol, "MyModuleBase", true)
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
        assertOCInterfaceSymbol(translatedSymbol, "", false)
    }

    fun `test stop translating after invalidation by adjacent file`() {
        val file = configure("class A")
        val translatedSymbol = translator.translate(file, TestTarget).single() as KtOCInterfaceSymbol
        assertFalse("state already loaded", translatedSymbol.stateLoaded)

        configure("typealias B = Unit", fileName = "other")

        assertEquals("MyModuleA", translatedSymbol.name)
        assertOCInterfaceSymbol(translatedSymbol, "", false)
    }

    fun `test file symbol table invalidation`() {
        val file = configure("class A")
        val virtualFile = file.virtualFile
        val context = contextForFile(virtualFile)

        val table = cache.forFile(virtualFile, context)
        assertNotNull("table could not be built", table)
        assertTrue("table not valid", table!!.isValid)

        val translatedSymbol = table.contents.single() as KtOCInterfaceSymbol
        assertEquals("MyModuleA", translatedSymbol.name)
        assertFalse("state already loaded", translatedSymbol.stateLoaded)
        assertOCInterfaceSymbol(translatedSymbol, "MyModuleBase", true)

        runWriteAction {
            val document = myFixture.getDocument(file)
            document.setText("class B")
            PsiDocumentManager.getInstance(project).commitDocument(document)
        }

        assertFalse("table still valid", table.isValid)

        val rebuiltTable = cache.forFile(virtualFile, context)
        assertNotNull("table was not rebuilt", rebuiltTable)
        assertTrue("table was not rebuilt", rebuiltTable!!.isValid)

        val translatedRebuiltSymbol = rebuiltTable.contents.single() as KtOCInterfaceSymbol
        assertEquals("MyModuleB", translatedRebuiltSymbol.name)
        assertFalse("state already loaded", translatedRebuiltSymbol.stateLoaded)
        assertOCInterfaceSymbol(translatedRebuiltSymbol, "MyModuleBase", true)
    }

    fun `test aborted file symbol table after invalidation by adjacent file`() {
        val file = configure("class A")
        val virtualFile = file.virtualFile
        val context = contextForFile(virtualFile)

        val table = cache.forFile(virtualFile, context)
        assertNotNull("table could not be built", table)
        assertTrue("table not valid", table!!.isValid)

        val translatedSymbol = table.contents.single() as KtOCInterfaceSymbol
        assertFalse("state already loaded", translatedSymbol.stateLoaded)

        configure("typealias B = Unit", fileName = "other")

        assertEquals("MyModuleA", translatedSymbol.name)
        assertOCInterfaceSymbol(translatedSymbol, "", false) // trigger translation, expect abort

        assertTrue("table not valid", table.isValid)
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
        assertFalse("table still valid", table.isValid)

        val rebuiltTable = cache.forFile(virtualFile, context)
        assertNotNull("table was not rebuilt", rebuiltTable)
        assertTrue("table was not rebuilt", rebuiltTable!!.isValid)

        val translatedRebuiltSymbol = rebuiltTable.contents.single() as KtOCInterfaceSymbol
        assertEquals("MyModuleA", translatedRebuiltSymbol.name)
        assertFalse("state already loaded", translatedRebuiltSymbol.stateLoaded)
        assertOCInterfaceSymbol(translatedRebuiltSymbol, "MyModuleBase", true)
    }

    fun `test not fully translated file symbol table after invalidation by adjacent file`() {
        val file = configure("class A")
        val virtualFile = file.virtualFile
        val context = contextForFile(virtualFile)

        val table = cache.forFile(virtualFile, context)
        assertNotNull("table could not be built", table)
        assertTrue("table not valid", table!!.isValid)

        val translatedSymbol = table.contents.single() as KtOCInterfaceSymbol

        configure("typealias B = Unit", fileName = "other")

        assertEquals("MyModuleA", translatedSymbol.name)
        file.getResolutionFacade().moduleDescriptor // simulate module descriptor invalidation by unrelated resolve

        assertTrue("table not valid", table.isValid)
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
        assertFalse("table still valid", table.isValid)

        val rebuiltTable = cache.forFile(virtualFile, context)
        assertNotNull("table was not rebuilt", rebuiltTable)
        assertTrue("table was not rebuilt", rebuiltTable!!.isValid)

        val translatedRebuiltSymbol = rebuiltTable.contents.single() as KtOCInterfaceSymbol
        assertEquals("MyModuleA", translatedRebuiltSymbol.name)
        assertFalse("state already loaded", translatedRebuiltSymbol.stateLoaded)
        assertOCInterfaceSymbol(translatedRebuiltSymbol, "MyModuleBase", true)

        assertFalse("state already loaded", translatedSymbol.stateLoaded)
        assertOCInterfaceSymbol(translatedSymbol, "", false) // original table should still abort
    }

    fun `test fully translated file symbol table after invalidation by adjacent file`() {
        val file = configure("class A")
        val virtualFile = file.virtualFile
        val context = contextForFile(virtualFile)

        val table = cache.forFile(virtualFile, context)
        assertNotNull("table could not be built", table)
        assertTrue("table not valid", table!!.isValid)

        val translatedSymbol = table.contents.single() as KtOCInterfaceSymbol
        assertFalse("state already loaded", translatedSymbol.stateLoaded)
        assertOCInterfaceSymbol(translatedSymbol, "MyModuleBase", true)

        configure("typealias B = Unit", fileName = "other")

        assertTrue("table not valid", table.isValid)
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
        assertTrue("table not valid", table.isValid)

        val rebuiltTable = cache.forFile(virtualFile, context)
        assertSame("table was rebuilt", table, rebuiltTable)
    }

    fun `test file symbol table not invalidated by in-code-block change`() {
        val file = configure("fun a() { <caret> }")
        val virtualFile = file.virtualFile
        val context = contextForFile(virtualFile)

        val table = cache.forFile(virtualFile, context)
        assertNotNull("table could not be built", table)
        assertTrue("table not valid", table!!.isValid)

        val translatedSymbol = table.contents.single() as KtOCInterfaceSymbol
        assertEquals("MyModuleToTranslateKt", translatedSymbol.name)
        assertFalse("state already loaded", translatedSymbol.stateLoaded)
        assertOCInterfaceSymbol(translatedSymbol, "MyModuleBase", true)

        runWriteAction {
            val document = myFixture.getDocument(file)
            document.setText("fun a() { print(\"Hello\") }")
            PsiDocumentManager.getInstance(project).commitDocument(document)
        }

        assertTrue("table no longer valid", table.isValid)
    }

    private fun contextForFile(virtualFile: VirtualFile): OCInclusionContext {
        assumeFalse("cache already contains file", cache.cachedFiles.contains(virtualFile))
        val bridgingFile = KonanBridgeFileManager.getInstance(project).forTarget(TestTarget, "testTarget.h")
        val psiBridgingFile = PsiManager.getInstance(project).findFile(bridgingFile) as KonanBridgePsiFile
        return OCInclusionContext.empty(CLanguageKind.OBJ_C, psiBridgingFile).apply { addProcessedFile(bridgingFile) }
    }

    private fun configure(code: String, fileName: String = "toTranslate"): KtFile =
        myFixture.configureByText("$fileName.kt", code) as KtFile

    private fun assertOCInterfaceSymbol(translatedSymbol: KtOCInterfaceSymbol, expectedSuperType: String, expectLoaded: Boolean) {
        assertFalse("unexpected template symbol", translatedSymbol.isTemplateSymbol)
        assertEquals("unexpected super type", expectedSuperType, translatedSymbol.superType.name)
        assertEquals("unexpected loaded state", expectLoaded, translatedSymbol.stateLoaded)
    }
}