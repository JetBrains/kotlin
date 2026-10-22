// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightAccessorMethod(getB)
// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightAccessorMethod(setB)
// KT-90199
data class X(val a: Int, val b: String)

class C {
    var (a, <caret>b) = X(1, "")
}
