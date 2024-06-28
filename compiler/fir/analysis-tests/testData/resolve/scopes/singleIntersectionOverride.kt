// ISSUE: KT-63738
// SCOPE_DUMP: D:foo

interface A<E1> {
    fun foo(): E1 // (1)
}

interface B<E2> : A<E2> {
    override fun foo(): E2 // (2)
}

interface C<E3> : A<E3> {
    // substitution-override fun foo(): E3 // (3)
}

interface D<E4> : B<E4>, C<E4> {
    // substitution-override fun foo(): E4 // (4)
}
