abstract class Base<T> {
    context(cp1: T, cp2: Boolean)
    abstract val foo: T
}

abstract class Another: Base<Long>()

fun Boolean.usage(another: Another) {
    with(0L) {
        another.<expr>foo</expr>
        Unit
    }
}

// WITH_STDLIB
// LANGUAGE: +ContextParameters
