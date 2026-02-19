abstract class Base<T> {
    context(cp1: T, cp2: Boolean)
    abstract fun foo(rp1: T, rp2: String)
}

abstract class Another: Base<Long>()

fun Boolean.usage(another: Another) {
    with(0L) {
        <expr>another.foo(1L, "str")</expr>
        Unit
    }
}

// WITH_STDLIB
// LANGUAGE: +ContextParameters
