class A<T>
class B<T>
class C<T>
class D<K, V>
class E
private fun test()<caret> = {
    class Local
    C<D<A<Local>, B<D<Local, A<E>>>>>()
}