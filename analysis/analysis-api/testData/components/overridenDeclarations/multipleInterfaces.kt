// FILE: main.kt
class A : B, C, D {
    override fun foo(x: Int) {
    }

    override fun foo<caret>(x: String) {
    }
}

// FILE: B.kt
interface B {
    fun foo(x: Int)
    fun foo(x: String)
}

// FILE: C.kt
interface C {
    fun foo(x: Int)
    fun foo(x: String)
}

// FILE: D.kt
interface D {
    fun foo(x: Int)
    fun foo(x: String)
}


// RESULT
// ALL:
// B.foo(x: String): Unit
// C.foo(x: String): Unit
// D.foo(x: String): Unit

// DIRECT:
// B.foo(x: String): Unit
// C.foo(x: String): Unit
// D.foo(x: String): Unit
