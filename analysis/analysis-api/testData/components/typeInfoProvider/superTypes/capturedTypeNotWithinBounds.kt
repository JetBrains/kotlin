class Inv<T>(val value: T) where T: CharSequence, T: Number

interface A
interface B
interface C<T>: A, B

fun <R> foo(i: Inv<in R>) where R: C<Int> {
    val x = <expr>i.value</expr>
}
