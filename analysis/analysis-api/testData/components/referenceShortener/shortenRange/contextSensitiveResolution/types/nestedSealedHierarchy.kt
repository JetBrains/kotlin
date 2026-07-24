// IDE_MODE
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType
package test

sealed class Outer {
    sealed class Mid1 : Outer() {
        class Leaf1 : Mid1()
        class Leaf2 : Mid1()
    }
    sealed class Mid2 : Outer()
}

fun testMid(m: Outer.Mid1) {
    when (m) {
        is <expr>Outer.Mid1.Leaf1</expr> -> {}
        is Outer.Mid1.Leaf2 -> {}
    }
}
