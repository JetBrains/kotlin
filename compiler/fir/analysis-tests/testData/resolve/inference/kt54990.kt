// ISSUE: KT-54990

class A<T1, T2: B<T1, Double>>
class B<T1, T2>
class C<L>(val x: A<out L, out B<L, Double>>)

fun test() {
    val x: A<out Any, out B<Any, Double>> = A()
    C<Any>(x)
}
