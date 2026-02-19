fun test(a: Any) {
    if (a is String) {
        a.<expr>foo()</expr>
    }
}

fun String.foo() {}