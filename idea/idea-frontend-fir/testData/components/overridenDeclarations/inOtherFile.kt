// FILE: main.kt
class A : B(){
    override fun fo<caret>o(x: Int): Int
}

// FILE: B.kt
abstract class B {
    open fun foo(x: Int): Int
    abstract fun foo(x: String): Int
}


// RESULT
// ALL:
// B.foo(x: Int): Int

// DIRECT:
// B.foo(x: Int): Int
