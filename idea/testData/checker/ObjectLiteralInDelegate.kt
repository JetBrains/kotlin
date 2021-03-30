// FIR_COMPARISON
// Test for KT-8187
interface A {
    fun get(x: Int)
}

class B : A by <error>object</error> : A {}

class C : A by (<error>object</error> : A {})

class D : A by 1 <error>+</error> (<error>object</error> : A {})

fun bar() {
    val <warning>e</warning> = object : A by <error>object</error> : A {} {}
}
