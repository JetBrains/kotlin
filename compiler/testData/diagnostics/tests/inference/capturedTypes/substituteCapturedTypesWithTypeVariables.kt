// FIR_IDENTICAL
// DIAGNOSTICS: -CAST_NEVER_SUCCEEDS -UNUSED_PARAMETER

class Foo<K>

fun <T> getFoo(value: T) = null as Foo<out Foo<T>>
fun <R> takeLambda(block: () -> R) {}

fun main(x: Int) {
    takeLambda {
        getFoo(x)
    }
}
