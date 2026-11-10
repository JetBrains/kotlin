// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightAccessorMethod(getValue)
// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightAccessorMethod(setValue)
// property: test/C.value
package test

interface I {
    var value: Int
        get() = 1
        set(v) {}
}

class C(i: I) : I by i
