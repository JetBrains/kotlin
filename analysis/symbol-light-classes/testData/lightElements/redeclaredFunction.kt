// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightSimpleMethod(first)
// EXPECTED_BY_PSI: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightSimpleMethod(first)
class C {
    @JvmName("first")
    fun fo<caret>o() {}

    @JvmName("second")
    fun foo() {}
}
