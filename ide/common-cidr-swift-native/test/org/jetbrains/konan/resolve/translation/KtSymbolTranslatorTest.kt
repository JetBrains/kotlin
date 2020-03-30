package org.jetbrains.konan.resolve.translation

import com.intellij.openapi.util.registry.Registry
import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.konan.resolve.konan.KonanTarget
import org.jetbrains.konan.resolve.symbols.objc.KtOCInterfaceSymbol
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.JUnit3WithIdeaConfigurationRunner
import org.junit.runner.RunWith

@RunWith(JUnit3WithIdeaConfigurationRunner::class)
class KtSymbolTranslatorTest : KotlinLightCodeInsightFixtureTestCase() {
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
        val file = myFixture.configureByText(
            "ClassToTranslate.kt", """
            class A
        """.trimIndent()
        ) as KtFile

        val translator = KtOCSymbolTranslator(project)
        val translatedSymbols = translator.translate(file, TestTarget).toList()
        val translatedSymbol = translatedSymbols.single() as KtOCInterfaceSymbol
        assertFalse("state already loaded", translatedSymbol.stateLoaded)
        assertEquals("MyModuleA", translatedSymbol.name)
        assertFalse(translatedSymbol.isTemplateSymbol)
        assertEquals("MyModuleBase", translatedSymbol.superType.name)
        assertTrue("state not loaded", translatedSymbol.stateLoaded)
    }
}