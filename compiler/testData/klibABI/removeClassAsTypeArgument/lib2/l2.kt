interface I<T> {
    val o: T
}

class D {
    fun stable(): I<C> = object : I<C> {
        override val o: C = C()
    }
    fun foo(): String = stable().o.o()

    fun exp(): I<E> = object : I<E> {
        override val o: E = TODO()
    }
    fun bar(): String = exp().o.e()
}
