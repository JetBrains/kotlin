enum class E {
    ENTRY;

    companion object {
        fun foo(): E = ENTRY
        fun bar(): Array<E> = <!UNRESOLVED_REFERENCE!>values<!>()
        fun baz(): E = <!UNRESOLVED_REFERENCE!>valueOf<!>("ENTRY")
        val valuez = <!UNRESOLVED_REFERENCE!>values<!>()
    }

    fun oof(): E = ENTRY
    fun rab(): Array<E> = values()
    fun zab(): E = valueOf("ENTRY")
}

fun foo() = E.ENTRY
fun bar() = E.values()
fun baz() = E.valueOf("ENTRY")
