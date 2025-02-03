abstract class Base<C, R> {
    context(c: C)
    abstract val R.foo: Boolean
}

fun <T> Boolean.usage(base: Base<Boolean, T>, param: T) {
    with(base) {
        param.<expr>foo</expr>
        Unit
    }
}

// WITH_STDLIB
// LANGUAGE: +ContextParameters
