class Out<out T>

interface X : <!INTERFACE_WITH_SUPERCLASS!>Out<String><!>

abstract class Base<E> {
    fun <D : Out<E>> f(t: MutableList<D>, e: MutableList<E>) {}
}

class C : Base<CharSequence>()

fun f(list: MutableList<X>, s: MutableList<CharSequence>) {
    C().f(list, s)
    C().f(<!ARGUMENT_TYPE_MISMATCH!>s<!>, <!ARGUMENT_TYPE_MISMATCH!>list<!>)
}
