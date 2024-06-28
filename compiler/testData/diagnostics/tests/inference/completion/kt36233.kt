// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER -USELESS_CAST

class Inv<T>
class Out<out T>

fun <K> foo(y: K?) = Inv<Out<K>>()
fun <R> test(x: Inv<Out<R>>) {}

fun main() {
    test<Int>(foo(null)) // type mismatch
    test<Number>(foo(1 as Int)) // type mismatch
}
