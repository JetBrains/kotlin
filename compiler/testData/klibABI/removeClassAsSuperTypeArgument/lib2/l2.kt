interface I<T> {
    val o: T
}

class ST : I<C> {
    override val o: C = C()
}

class EX: I<E> {
    override val o: E = TODO()
}

class D {
    fun stable(): ST = ST()
    fun foo(): String = stable().o.o()

    fun exp(): EX = EX()
    fun bar(): String = exp().o.e()
}
