fun test(a: (String) -> Unit = {}, b: (String) -> Unit = {}) {
    a("a")
    b("b")
}

fun foo() {
    test({ }, { }<caret>)
}