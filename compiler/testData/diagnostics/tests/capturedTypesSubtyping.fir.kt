// ISSUE: KT-62959

fun bar(x: Inv<out CharSequence>) {
    x.foo { it }
    x.bar { it.e() }
}

interface Inv<E> {
    fun foo(x: (E) -> E) {}
    fun bar(x: (Inv<E>) -> E) {}

    fun e(): E = null!!
}
