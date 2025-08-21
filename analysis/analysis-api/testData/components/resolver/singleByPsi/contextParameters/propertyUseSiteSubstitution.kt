abstract class Base<T> {
    context(_: T, cp2: Boolean)
    abstract val foo: T
}

fun Boolean.usage(base: Base<Long>) {
    with(0L) {
        base.<expr>foo</expr>
        Unit
    }
}

// WITH_STDLIB
// LANGUAGE: +ContextParameters
