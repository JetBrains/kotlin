abstract class Base<T> {
    context(_: T, cp2: Boolean)
    abstract fun foo(rp1: T, rp2: String)
}

fun Boolean.usage(base: Base<Long>) {
    with(0L) {
        base.<expr>foo(1L, "str")</expr>
        Unit
    }
}

// WITH_STDLIB
// LANGUAGE: +ContextParameters
