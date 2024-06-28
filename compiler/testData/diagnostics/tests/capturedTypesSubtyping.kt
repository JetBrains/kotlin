// ISSUE: KT-62959

fun bar(x: Inv<out CharSequence>) {
    x.foo { <!TYPE_MISMATCH("Nothing; CharSequence"), TYPE_MISMATCH("Nothing; CharSequence")!>it<!> }
    x.bar { <!TYPE_MISMATCH("Nothing; CharSequence"), TYPE_MISMATCH("Nothing; CharSequence")!>it.e()<!> }
}

interface Inv<E> {
    fun foo(x: (E) -> E) {}
    fun bar(x: (Inv<E>) -> E) {}

    fun e(): E = null!!
}
