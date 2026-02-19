interface Foo {
    fun Bar.foo()
}

interface Bar {}

inline fun <T, R> myWith(argument: T, lambda: T.() -> R): R {
    argument.lambda()
}

fun Any.action(other: Any) {
    if (this is Foo) {
        with(other) {
            this as Bar
            <expr>foo</expr>()
        }
    }
}