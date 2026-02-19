class A {
    fun foo(a: String): String = a
}

class Base {
    context(a: A)
    fun funMember() {
        a.foo("")
    }

    context(a: A)
    fun usageInsideClass() {
        <expr>funMember()</expr>
    }
}

// LANGUAGE: +ContextParameters
