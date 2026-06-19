class Inv<T>(val value: T?)

fun foo(i: Inv<out CharSequence>) {
    val x = <expr>i.value</expr>
}
