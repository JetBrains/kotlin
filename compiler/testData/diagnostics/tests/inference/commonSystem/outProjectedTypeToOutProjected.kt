// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER

class Inv<T>

fun <K> select(x: K, y: K): K = x

fun <V> outToOut(x: Inv<out V>): Inv<out V> = TODO()

fun test(invOutAny: Inv<out Any>, invAny: Inv<Any>) {
    val a: Inv<out Any> = select(invAny, outToOut(invOutAny))
}
