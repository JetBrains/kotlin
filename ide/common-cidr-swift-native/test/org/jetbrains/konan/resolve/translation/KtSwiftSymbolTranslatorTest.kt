package org.jetbrains.konan.resolve.translation

import com.jetbrains.swift.psi.types.SwiftFunctionType
import com.jetbrains.swift.psi.types.SwiftOptionalType
import com.jetbrains.swift.symbols.SwiftFunctionSymbol
import org.jetbrains.konan.resolve.symbols.swift.KtSwiftClassSymbol
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance

class KtSwiftSymbolTranslatorTest : KtSymbolTranslatorTestCase() {
    fun `test simple class translation`() {
        val file = configure("class A")
        val translatedSymbols = KtSwiftSymbolTranslator.translate(file)
        assertSize(2, translatedSymbols)

        val translatedSymbol = translatedSymbols.first() as KtSwiftClassSymbol
        assertEquals("A", translatedSymbol.name)
        assertEquals("A", translatedSymbol.qualifiedName)
        assertFalse("state already loaded", translatedSymbol.stateLoaded)
        assertSwiftInterfaceSymbol(translatedSymbol, "MyModuleBase", true, null)
    }

    fun `test nested class translation`() {
        val file = configure("class A { class B }")
        val translatedSymbols = KtSwiftSymbolTranslator.translate(file)
        assertSize(2, translatedSymbols)

        val translatedSymbol = translatedSymbols.first() as KtSwiftClassSymbol
        val nestedSymbol = translatedSymbol.members.firstIsInstance<KtSwiftClassSymbol>()
        assertSwiftInterfaceSymbol(translatedSymbol, "MyModuleBase", true, null, nestedSymbol)
        assertEquals("B", nestedSymbol.name)
        assertEquals("A.B", nestedSymbol.qualifiedName)
        assertFalse("state already loaded", nestedSymbol.stateLoaded)
        assertSwiftInterfaceSymbol(nestedSymbol, "MyModuleBase", true, translatedSymbol)
    }

    fun `test meta class type translation`() {
        val file = configure("import kotlinx.cinterop.ObjCClass; class A { fun a(c: ObjCClass?) {}; fun b(c: ObjCClass) {} }")
        val translatedSymbols = KtSwiftSymbolTranslator.translate(file)
        assertSize(2, translatedSymbols)

        val translatedSymbol = translatedSymbols.first() as KtSwiftClassSymbol
        val (a, b) = translatedSymbol.members.filterIsInstance<SwiftFunctionSymbol>().sortedBy { it.name }

        assertEquals("a", a.name)
        val aParam = (a.swiftType.image as SwiftFunctionType).domain.items.single()
        assertEquals("a ref name", "AnyClass", (aParam.swiftType as SwiftOptionalType).component.presentableText)

        assertEquals("b", b.name)
        val bParam = (b.swiftType.image as SwiftFunctionType).domain.items.single()
        assertEquals("b ref name", "AnyClass", bParam.swiftType.presentableText)
    }
}