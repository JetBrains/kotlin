interface I<F> {
    fun <T : Comparable<T>> f(t: List<T>, f: List<F>): Any// T = D, List<D> == List<D>
}

abstract class Base<E> {
    fun <D : Comparable<D>> f(t: List<D>, e: List<E>) {}
}


class C : Base<String>(), I<String>

fun f(list: List<Int>, s: List<String>) {
    C().f(list, s)
}
