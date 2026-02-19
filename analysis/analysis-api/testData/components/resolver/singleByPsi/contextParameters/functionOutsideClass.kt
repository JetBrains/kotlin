class A {
    fun foo(a: String): String = a
}

class Base {
    context(a: A)
    fun funMember() {
        a.foo("")
    }
}

fun usageOutsideClass() {
    with(Base()) {
        with(A()) {
            <expr>funMember()</expr>
            1
        }
    }
}

// LANGUAGE: +ContextParameters
