class Inv<T>(val value: T) where T: CharSequence, T: A

interface A
interface B
interface C<T>: A, B, CharSequence

fun <R> foo(i: Inv<in R>) where R: C<Int> {
    val x = <expr>i.value</expr>
}
