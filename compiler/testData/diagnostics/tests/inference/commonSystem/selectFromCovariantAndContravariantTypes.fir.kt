// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER

class In<in T>
class Out<out T>

class A
class B

fun <K> select(x: K, y: K): K = x
fun <V> genericIn(x: In<V>) {}
fun <V> genericOut(x: Out<V>) {}

fun test1(a: In<A>, b: In<B>) {
    genericIn(select(a, b))
}

fun test2(a: Out<A>, b: Out<B>) {
    genericOut(select(a, b))
}
