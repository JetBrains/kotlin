class Inv<T>(val value: T)

fun foo(i: Inv<in CharSequence>) {
    val x = <expr>i.value</expr>
}
