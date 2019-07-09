interface I {
    fun <T : Comparable<T>> f(t: List<T>): Any// T = D, List<D> == List<D>
}

abstract class Base {
    fun <D : Comparable<D>> f(t: List<D>) {}
}


class C : Base(), I


fun f(list: List<Int>) {
    C().f(list)
}