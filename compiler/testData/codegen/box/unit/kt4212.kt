fun foo(): Any? = bar()

fun bar() {}

fun baz(): Any? {
    return bar()
}

fun quux(): Unit? = bar()

fun box(): String {
    foo()

    if (foo() != Unit) return "Fail 1"
    if (foo() != bar()) return "Fail 2"
    if (bar() != baz()) return "Fail 3"
    if (baz() != quux()) return "Fail 4"

    return "OK"
}
