// !DIAGNOSTICS: -UNUSED_VARIABLE
interface T<E> {
    fun f() : E = null!!
}
open class A<X>() {
    inner class B() : T<X> {}
}

fun test() {
    val a = A<Int>()
    val b : A<Int>.B = a.B()
}