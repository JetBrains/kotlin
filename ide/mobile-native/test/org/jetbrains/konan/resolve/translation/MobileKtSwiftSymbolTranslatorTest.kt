package org.jetbrains.konan.resolve.translation

import com.jetbrains.swift.psi.types.SwiftFunctionType
import com.jetbrains.swift.psi.types.SwiftOptionalType
import com.jetbrains.swift.symbols.SwiftFunctionSymbol
import org.jetbrains.konan.resolve.symbols.swift.KtSwiftClassSymbol

class MobileKtSwiftSymbolTranslatorTest : KtSymbolTranslatorTestCase() {
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