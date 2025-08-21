abstract class Base<C, R, V> {
    context(c: C)
    abstract fun R.foo(v: V)
}

abstract class Another<A, B, C>: Base<A, B, C>()

context(_: Int)
fun Boolean.usage(another: Another<Int, String, Boolean>) {
    with(another) {
        val s = "str"
        <expr>s.foo(this@usage)</expr>
        Unit
    }
}

// WITH_STDLIB
// LANGUAGE: +ContextParameters
