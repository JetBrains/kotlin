package org.jetbrains.konan.resolve.translation

import org.jetbrains.konan.resolve.symbols.swift.KtSwiftClassSymbol
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance

class KtSwiftSymbolTranslatorTest : KtSymbolTranslatorTestCase() {
    override val translator: KtFileTranslator<*, *>
        get() = KtSwiftSymbolTranslator(project)

    fun `test simple class translation`() {
        val file = configure("class A")
        val translatedSymbol = translator.translate(file, TestTarget.productModuleName).single() as KtSwiftClassSymbol
        assertEquals("A", translatedSymbol.name)
        assertEquals("A", translatedSymbol.qualifiedName)
        assertFalse("state already loaded", translatedSymbol.stateLoaded)
        assertSwiftInterfaceSymbol(translatedSymbol, "MyModuleBase", true, null)
    }

    fun `test nested class translation`() {
        val file = configure("class A { class B }")
        val translatedSymbol = translator.translate(file, TestTarget.productModuleName).single() as KtSwiftClassSymbol
        val nestedSymbol = translatedSymbol.members.firstIsInstance<KtSwiftClassSymbol>()
        assertSwiftInterfaceSymbol(translatedSymbol, "MyModuleBase", true, null, nestedSymbol)
        assertEquals("B", nestedSymbol.name)
        assertEquals("A.B", nestedSymbol.qualifiedName)
        assertFalse("state already loaded", nestedSymbol.stateLoaded)
        assertSwiftInterfaceSymbol(nestedSymbol, "MyModuleBase", true, translatedSymbol)
    }
}