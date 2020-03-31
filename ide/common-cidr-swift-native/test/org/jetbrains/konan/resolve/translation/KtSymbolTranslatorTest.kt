package org.jetbrains.konan.resolve.translation

import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.PsiDocumentManager
import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.konan.resolve.konan.KonanTarget
import org.jetbrains.konan.resolve.symbols.objc.KtOCInterfaceSymbol
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.JUnit3WithIdeaConfigurationRunner
import org.jetbrains.kotlin.test.testFramework.runWriteAction
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

    override fun setUp() {
        super.setUp()
        Registry.get("cidr.memory.efficient.interner").setValue(false)
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

    private fun configure(code: String, fileName: String = "toTranslate"): KtFile =
        myFixture.configureByText("$fileName.kt", code) as KtFile
}