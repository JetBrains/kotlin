package org.jetbrains.konan.resolve.translation

import com.jetbrains.cidr.lang.symbols.objc.OCMethodSymbol
import com.jetbrains.cidr.lang.types.OCNullability
import com.jetbrains.cidr.lang.types.OCPointerType
import org.jetbrains.konan.resolve.symbols.objc.KtOCInterfaceSymbol

class KtOCSymbolTranslatorTest : KtSymbolTranslatorTestCase() {
    fun `test simple class translation`() {
        val file = configure("class A")
        val translatedSymbols = KtOCSymbolTranslator.translate(file)
        assertSize(2, translatedSymbols)

        val translatedSymbol = translatedSymbols.first() as KtOCInterfaceSymbol
        assertEquals("MyModuleA", translatedSymbol.name)
        assertFalse("state already loaded", translatedSymbol.stateLoaded)
        assertOCInterfaceSymbol(translatedSymbol, "MyModuleBase", true)
    }

    fun `test nested class translation`() {
        val file = configure("class A { class B }")
        val translatedSymbols = KtOCSymbolTranslator.translate(file)
        assertSize(3, translatedSymbols)

        val nestedSymbol = translatedSymbols[1] as KtOCInterfaceSymbol
        assertEquals("MyModuleAB", nestedSymbol.name)
        assertFalse("state already loaded", nestedSymbol.stateLoaded)
        assertOCInterfaceSymbol(nestedSymbol, "MyModuleBase", true)
    }

    fun `test NSString pointer nullability`() {
        val file = configure("class A { fun a(): String? = \"a\"; fun b(): String = \"b\" }")
        val translatedSymbols = KtOCSymbolTranslator.translate(file)
        assertSize(2, translatedSymbols)

        val translatedSymbol = translatedSymbols.first() as KtOCInterfaceSymbol
        val (a, b) = translatedSymbol.members
            .filterIsInstance<OCMethodSymbol>().filter { it.name == "a" || it.name == "b" }.sortedBy { it.name }

        assertEquals("a", a.name)
        val aReturn = a.getReturnType(project) as OCPointerType
        assertEquals("a pointer nullability", OCNullability.NULLABLE, aReturn.nullability)
        assertEquals("a ref name", "NSString", aReturn.refType.name)
        assertEquals("a ref nullability", OCNullability.NONNULL, aReturn.refType.nullability)

        assertEquals("b", b.name)
        val bReturn = b.getReturnType(project) as OCPointerType
        assertEquals("b pointer nullability", OCNullability.NONNULL, bReturn.nullability)
        assertEquals("b ref name", "NSString", bReturn.refType.name)
        assertEquals("b ref nullability", OCNullability.NONNULL, bReturn.refType.nullability)
    }
}