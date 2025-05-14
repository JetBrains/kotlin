// RUN_PIPELINE_TILL: FRONTEND

class Example {
    static fun foo(): Int = 1
    static fun bar(): Int = foo()
    static fun moo(): Int = <!UNRESOLVED_REFERENCE!>memberFunction<!>()

    fun memberFunction(): Int = 0

    companion object {
        fun foo(): String = ""
    }
}

fun example() {
    val x1: Int = Example.foo()
    val x2: String = <!INITIALIZER_TYPE_MISMATCH!>Example.foo()<!>

    val e = Example()
    val y = e.<!UNRESOLVED_REFERENCE!>foo<!>()
}