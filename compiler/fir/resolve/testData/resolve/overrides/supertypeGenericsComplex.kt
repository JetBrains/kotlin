class Out<out T>

interface X : Out<String>

abstract class Base<E> {
    fun <D : Out<E>> f(t: MutableList<D>, e: MutableList<E>) {}
}

class C : Base<CharSequence>()

fun f(list: MutableList<X>, s: MutableList<CharSequence>) {
    C().f(list, s)
    C().<!INAPPLICABLE_CANDIDATE!>f<!>(s, list)
}
