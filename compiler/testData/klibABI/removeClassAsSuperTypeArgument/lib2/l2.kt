
interface I<T> {
    val o: T
}

class ST : I<C> {
    override val o: C = C()
}

class EX: I<E> {
    override val o: E = E()
}

class D {
    fun foo(): String = stable().o.o()
    fun stable(): ST = ST()

    fun bar(): String = exp().o.e()
    fun exp(): EX = EX()
}