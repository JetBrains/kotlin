// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightSimpleMethod(foo)
// function: test/C.foo
package test

interface I {
    fun foo(): Int = 1
}

class C(i: I) : I by i
