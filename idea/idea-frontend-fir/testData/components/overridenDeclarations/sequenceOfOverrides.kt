// FILE: main.kt
class A : B() {
    override fun foo<caret>(x: Int) {}
}

// FILE: B.kt
open class B : C() {
    override fun foo(x: Int) {}
}

// FILE: C.kt
open class C : D() {
    override fun foo(x: Int) {}
}

// FILE: D.kt
open class D {
    open fun foo(x: Int) {}
}


// RESULT
// ALL:
// B.foo(x: Int): Unit
// C.foo(x: Int): Unit
// D.foo(x: Int): Unit

// DIRECT:
// B.foo(x: Int): Unit
