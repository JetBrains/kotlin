package org.jetbrains.konan.resolve.translation

import org.jetbrains.konan.resolve.symbols.objc.KtOCInterfaceSymbol

class KtOCSymbolTranslatorTest : KtSymbolTranslatorTestCase() {
    fun `test simple class translation`() {
        val file = configure("class A")
        val translatedSymbol = translator.translate(file, TestTarget.productModuleName).single() as KtOCInterfaceSymbol
        assertEquals("MyModuleA", translatedSymbol.name)
        assertFalse("state already loaded", translatedSymbol.stateLoaded)
        assertOCInterfaceSymbol(translatedSymbol, "MyModuleBase", true)
    }
}