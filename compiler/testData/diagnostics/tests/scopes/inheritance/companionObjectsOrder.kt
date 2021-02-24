// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE
open class A {
    companion object {
        fun bar() = 1
    }
    init {
        val a: Int = foo()
        val b: Int = bar()
    }
}

open class B: A() {
    companion object {
        fun bar() = ""
    }
    init {
        val a: String = foo()
        val b: String = bar()
    }
}

fun A.Companion.foo() = 1
fun B.Companion.foo() = ""

class C: A() {
    init {
        val a: Int = foo()
        val b: Int = bar()
    }
}

class D: B() {
    init {
        val a: String = foo()
        val b: String = bar()
    }
}