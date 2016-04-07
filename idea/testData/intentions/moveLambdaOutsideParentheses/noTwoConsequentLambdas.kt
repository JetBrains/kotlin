fun bar() {
    foo { "one" } (<caret>{ "two" })
}

fun foo(a: () -> String): (() -> String) -> Unit {
    return { }
}
