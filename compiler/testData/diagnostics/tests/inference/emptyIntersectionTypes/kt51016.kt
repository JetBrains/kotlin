// FIR_IDENTICAL
interface A<T>
interface B<T> : A<T>

fun <T : Comparable<T>, S : T?> B<in S>.foo(t: T) {}
fun <T : Comparable<T>, S : T?> A<in S>.foo(other: A<in S>) {}

interface C<T> : B<T>, Comparable<C<*>>

fun test(x: C<Long?>) {
    x.foo(x)  // OVERLOAD_RESOLUTION_AMBIGUITY, shoub be OK
}
