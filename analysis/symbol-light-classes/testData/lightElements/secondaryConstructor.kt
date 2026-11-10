// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightConstructor(C)
// EXPECTED_BY_PSI: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightConstructor(C)
class C(val a: Int) {
    construc<caret>tor(a: String) : this(a.length)
}
