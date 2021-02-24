// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE
open class A {
    companion object {
        fun foo() = 1
        fun bar(a: String) = a
    }
}

open class B: A() {
    companion object {
        fun foo() = ""
        fun bar(a: Int) = a
    }
}

class C: B() {
    init {
        val a: String = foo()
        val b: Int = bar(1)
        val c: String = bar("")
    }
}