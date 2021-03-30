class A<T>
class B<T>
class C<T>
class D<K, V>
class E
private fun test(): () -> C<out D<out A<out Any>, out B<out D<out Any, A<E>>>>> = {
    class Local
    C<D<A<Local>, B<D<Local, A<E>>>>>()
}