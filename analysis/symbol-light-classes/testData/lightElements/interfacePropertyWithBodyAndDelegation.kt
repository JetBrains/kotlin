// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightAccessorMethod(getValue)
// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightAccessorMethod(setValue)
// EXPECTED_BY_PSI: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightAccessorMethod(getValue)
// EXPECTED_BY_PSI: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightAccessorMethod(setValue)
interface I {
    var val<caret>ue: Int
        get() = 1
        set(v) {}
}

class C(i: I) : I by i
