//!LANGUAGE: +DefinitelyNonNullableTypes
// SKIP_KT_DUMP
// FIR_IDENTICAL

class In<in I>

fun <S> select(x: S, y: S, z: S): S = x

fun <T> foo(a: Array<In<T & Any>>, b: Array<In<String>>, c: Array<In<T>>) =
    select(a, b, c)[0].ofType(true)

inline fun <reified K> In<K>.ofType(y: Any?) =
    y is K

fun test() {
    val a1 = arrayOf(In<Int>())
    val a2 = arrayOf(In<String>())
    val a3 = arrayOf(In<Int>())
    foo(a1, a2, a3)
}