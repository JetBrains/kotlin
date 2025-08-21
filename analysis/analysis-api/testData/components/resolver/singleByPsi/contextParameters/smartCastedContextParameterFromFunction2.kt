class A

context(_: A)
fun foo() {

}

context(p: T)
fun <T> usage() {
    if (p is A) {
        <expr>foo()</expr>
        Unit
    }
}

// LANGUAGE: +ContextParameters
