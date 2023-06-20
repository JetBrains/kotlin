// ORIGINAL: /compiler/testData/diagnostics/tests/override/DefaultParameterValueInOverride.fir.kt
// WITH_STDLIB
open class A {
    open fun foo(a : Int) {}
}

class C : A() {
    override fun foo(a : Int = 1) {
    }
}

class D : A() {
    override fun foo(a : Int = 1) {
    }
}


fun box() = "OK"
