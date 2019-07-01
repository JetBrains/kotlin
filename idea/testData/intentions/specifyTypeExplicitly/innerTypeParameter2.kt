class A<T>
class B<T>
class C<T>
class D<K, V>
class E
private fun test()<caret> = {
    C<D<A<E>, B<D<C<E>, A<E>>>>>()
}