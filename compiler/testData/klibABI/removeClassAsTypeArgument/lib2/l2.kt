
interface I<T> {
    val o: T
}

class D {
    fun foo(): String = stable().o.o()
    fun stable(): I<C> = object : I<C> {
        override val o: C = C()
    }

    fun bar(): String = exp().o.e()
    fun exp(): I<E> = object : I<E> {
        override val o: E = E()
    }
}