


class D {
    fun foo(): String = stable().o()
    fun stable(): C = C()

    fun bar(): String = exp().e()
    fun exp(): E = E()
}