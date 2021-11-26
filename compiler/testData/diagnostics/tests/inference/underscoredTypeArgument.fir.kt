// !LANGUAGE: -PartiallySpecifiedTypeArguments
// !DIAGNOSTICS: -UNCHECKED_CAST
// WITH_STDLIB

fun <K, T> foo(x: (K) -> T): Pair<K, T> = (1 as K) to (1f as T)

fun box(): String {
    val x = foo<Int, _> { it.toFloat() } // Pair<Int, Float>
    return "OK"
}
