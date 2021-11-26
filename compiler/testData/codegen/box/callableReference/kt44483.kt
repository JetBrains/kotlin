// WITH_STDLIB

fun <K, V> f(vararg p: Pair<K, V>): K = p[0].first

fun box(): String {
    // NB this is not an adapted function reference
    val f: (Array<out Pair<String, Int>>) -> String = ::f
    return f(arrayOf("OK" to 0))
}