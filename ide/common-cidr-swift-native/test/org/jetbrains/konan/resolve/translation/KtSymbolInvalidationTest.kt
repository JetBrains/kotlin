package org.jetbrains.konan.resolve.translation

import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.psi.PsiDocumentManager
import com.intellij.testFramework.PlatformTestUtil
import com.jetbrains.cidr.lang.symbols.symtable.FileSymbolTable
import org.jetbrains.konan.resolve.KtDependencyGraph
import org.jetbrains.konan.resolve.symbols.objc.KtOCInterfaceSymbol
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade

class KtSymbolInvalidationTest : KtSymbolTranslatorTestCase() {
    override fun runInDispatchThread(): Boolean = false

    fun `test stop translating after invalidation`(): Unit = invokeAndWaitIfNeeded {
        val file = configure("class A")
        val translatedSymbols = KtOCSymbolTranslator.translate(file)
        assertSize(2, translatedSymbols)

        val translatedSymbol = translatedSymbols.first() as KtOCInterfaceSymbol
        assertFalse("state already loaded", translatedSymbol.stateLoaded)

        runWriteAction {
            val document = myFixture.getDocument(file)
            document.setText("class B")
            PsiDocumentManager.getInstance(project).commitDocument(document)
        }

        assertEquals("MyModuleA", translatedSymbol.name)
        assertOCInterfaceSymbol(translatedSymbol, "", false)
    }

    fun `test stop translating after invalidation by adjacent file`(): Unit = invokeAndWaitIfNeeded {
        val file = configure("class A")
        val translatedSymbols = KtOCSymbolTranslator.translate(file)
        assertSize(2, translatedSymbols)

        val translatedSymbol = translatedSymbols.first() as KtOCInterfaceSymbol
        assertFalse("state already loaded", translatedSymbol.stateLoaded)

        configure("typealias B = Unit", fileName = "other")

        assertEquals("MyModuleA", translatedSymbol.name)
        assertOCInterfaceSymbol(translatedSymbol, "", false)
    }

    fun `test file symbol table invalidation`(): Unit = invokeAndWaitIfNeeded {
        val file = configure("class A")
        val virtualFile = file.virtualFile
        val context = contextForFile(virtualFile)

        val table = cache.forFile(virtualFile, context)
        assertNotNull("table could not be built", table)
        assertTrue("table not valid", table!!.isValid)
        assertSize(2, table.contents)

        val translatedSymbol = table.contents.first() as KtOCInterfaceSymbol
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
        assertSize(2, rebuiltTable.contents)

        val translatedRebuiltSymbol = rebuiltTable.contents.first() as KtOCInterfaceSymbol
        assertEquals("MyModuleB", translatedRebuiltSymbol.name)
        assertFalse("state already loaded", translatedRebuiltSymbol.stateLoaded)
        assertOCInterfaceSymbol(translatedRebuiltSymbol, "MyModuleBase", true)
    }

    fun `test aborted file symbol table after invalidation by adjacent file`(): Unit = invokeAndWaitIfNeeded {
        val file = configure("class A")
        val virtualFile = file.virtualFile
        val context = contextForFile(virtualFile)

        val table = cache.forFile(virtualFile, context)
        assertNotNull("table could not be built", table)
        assertTrue("table not valid", table!!.isValid)
        assertSize(2, table.contents)

        val translatedSymbol = table.contents.first() as KtOCInterfaceSymbol
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
        assertSize(2, rebuiltTable.contents)

        val translatedRebuiltSymbol = rebuiltTable.contents.first() as KtOCInterfaceSymbol
        assertEquals("MyModuleA", translatedRebuiltSymbol.name)
        assertFalse("state already loaded", translatedRebuiltSymbol.stateLoaded)
        assertOCInterfaceSymbol(translatedRebuiltSymbol, "MyModuleBase", true)
    }

    fun `test not fully translated file symbol table after invalidation by adjacent file`(): Unit = invokeAndWaitIfNeeded {
        val file = configure("class A")
        val virtualFile = file.virtualFile
        val context = contextForFile(virtualFile)

        val table = cache.forFile(virtualFile, context)
        assertNotNull("table could not be built", table)
        assertTrue("table not valid", table!!.isValid)
        assertSize(2, table.contents)

        val translatedSymbol = table.contents.first() as KtOCInterfaceSymbol

        configure("typealias B = Unit", fileName = "other")

        assertEquals("MyModuleA", translatedSymbol.name)
        file.getResolutionFacade().moduleDescriptor // simulate module descriptor invalidation by unrelated resolve

        assertTrue("table not valid", table.isValid)
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
        assertFalse("table still valid", table.isValid)

        val rebuiltTable = cache.forFile(virtualFile, context)
        assertNotNull("table was not rebuilt", rebuiltTable)
        assertTrue("table was not rebuilt", rebuiltTable!!.isValid)
        assertSize(2, rebuiltTable.contents)

        val translatedRebuiltSymbol = rebuiltTable.contents.first() as KtOCInterfaceSymbol
        assertEquals("MyModuleA", translatedRebuiltSymbol.name)
        assertFalse("state already loaded", translatedRebuiltSymbol.stateLoaded)
        assertOCInterfaceSymbol(translatedRebuiltSymbol, "MyModuleBase", true)

        assertFalse("state already loaded", translatedSymbol.stateLoaded)
        assertOCInterfaceSymbol(translatedSymbol, "", false) // original table should still abort
    }

    fun `test fully translated file symbol table after invalidation by adjacent file`() {
        val (table, context) = invokeAndWaitIfNeeded {
            val file = configure("class A")
            val virtualFile = file.virtualFile

            val context = contextForFile(virtualFile)
            val table = cache.forFile(virtualFile, context)
            assertNotNull("table could not be built", table)
            assertTrue("table not valid", table!!.isValid)
            assertSize(2, table.contents)

            val translatedSymbol = table.contents.first() as KtOCInterfaceSymbol
            assertFalse("state already loaded", translatedSymbol.stateLoaded)
            assertOCInterfaceSymbol(translatedSymbol, "MyModuleBase", true)
            configure("typealias B = Unit", fileName = "other")

            table to context
        }

        assertTrue("table not valid", table.isValid)
        KtDependencyGraph.getInstance(project).waitForPendingInvalidation()
        assertFalse("table still valid", table.isValid)

        runReadAction {
            val rebuiltTable = cache.forFile(table.containingFile, context)
            assertNotNull("table was not rebuilt", rebuiltTable)
            assertTrue("table was not rebuilt", rebuiltTable!!.isValid)
            assertSize(2, rebuiltTable.contents)

            val translatedRebuiltSymbol = rebuiltTable.contents.first() as KtOCInterfaceSymbol
            assertEquals("MyModuleA", translatedRebuiltSymbol.name)
            assertFalse("state already loaded", translatedRebuiltSymbol.stateLoaded)
            assertOCInterfaceSymbol(translatedRebuiltSymbol, "MyModuleBase", true)
        }
    }

    fun `test file symbol table not invalidated by in-code-block change`() {
        val table: FileSymbolTable = invokeAndWaitIfNeeded {
            val file = configure("fun a() { <caret> }")
            val virtualFile = file.virtualFile

            val context = contextForFile(virtualFile)
            val table = cache.forFile(virtualFile, context)
            assertNotNull("table could not be built", table)
            assertTrue("table not valid", table!!.isValid)
            assertSize(2, table.contents)

            val translatedSymbol = table.contents.first() as KtOCInterfaceSymbol
            assertEquals("MyModuleToTranslateKt", translatedSymbol.name)
            assertFalse("state already loaded", translatedSymbol.stateLoaded)
            assertOCInterfaceSymbol(translatedSymbol, "MyModuleBase", true)

            runWriteAction {
                val document = myFixture.getDocument(file)
                document.setText("fun a() { print(\"Hello\") }")
                PsiDocumentManager.getInstance(project).commitDocument(document)
            }

            table
        }

        assertTrue("table not valid", table.isValid)
        // TODO exclude from Vfs invalidation, but still respond to adjacent files
        // KtDependencyGraph.getInstance(project).waitForPendingInvalidation()
        // assertTrue("table no longer valid", table.isValid)
    }
}