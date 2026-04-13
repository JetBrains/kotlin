class A {
    fun foo() {

    }
}

context(p: T)
fun <T> usage() {
    if (p is A) {
        p.<expr>foo()</expr>
        Unit
    }
}

// LANGUAGE: +ContextParameters
