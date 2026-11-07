class A<A> {
    companion object {
        class B<B> {
            inner class C<C> {
                inner class D<D>
            }
        }
    }
}

fun foo() {
    val xx = A.Companion.B<Int>().C<Int>().D<Int>()

    x<caret_type>x.toString()
}
