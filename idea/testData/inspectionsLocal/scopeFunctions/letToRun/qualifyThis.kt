// WITH_RUNTIME

class C {
    fun foo() = "c"
    val bar = "C"
}

class D {
    fun foo() = "d"
    val bar = "D"

    val x = C().<caret>let {
        foo() + it.foo() + bar + it.bar
    }
}
