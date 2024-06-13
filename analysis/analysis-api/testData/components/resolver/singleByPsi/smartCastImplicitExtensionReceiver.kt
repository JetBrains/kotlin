fun Any.test() {
    if (this is String) {
        <expr>foo()</expr>
    }
}

fun String.foo() {}
