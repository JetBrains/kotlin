// !LANGUAGE: +PartiallySpecifiedTypeArguments
// WITH_STDLIB

fun <K, T> foo(x: (K) -> T): Pair<K, T> = (1 as K) to (1f as T)

//class `_` {}

fun box(): String {
    val x = foo<Int, _> { it.toFloat() } // Pair<Int, Float>
    return "OK"
}
