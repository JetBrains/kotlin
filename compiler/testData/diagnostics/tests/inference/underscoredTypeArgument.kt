// FIR_IDENTICAL
// !DIAGNOSTICS: -UNCHECKED_CAST
// WITH_RUNTIME

fun <K, T> foo(x: (K) -> T): Pair<K, T> = (1 as K) to (1f as T)

fun box(): String {
    val x = foo<Int, <!UNRESOLVED_REFERENCE!>_<!>> { it.toFloat() } // Pair<Int, Float>
    return "OK"
}
