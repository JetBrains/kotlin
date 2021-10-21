// WITH_RUNTIME

fun <K, T> foo(x: (K) -> T): Pair<K, T> = (1 as K) to (1f as T)

class `_` {}

fun box(): String {
    val x1 = foo<Int, `_`> { it.toFloat() as `_` } // Pair<Int, Float>
    return "OK"
}
