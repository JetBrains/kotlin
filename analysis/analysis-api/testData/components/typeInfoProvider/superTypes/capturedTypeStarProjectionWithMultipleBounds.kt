class Inv<T, R>(val value: T) where T: CharSequence, T: Number?, T: C<R>

interface A
interface B
interface C<T>: A, B

fun foo(i: Inv<*, *>) {
    val x = <expr>i.value</expr>
}
