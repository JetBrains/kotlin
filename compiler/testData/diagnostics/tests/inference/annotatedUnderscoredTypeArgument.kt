// !DIAGNOSTICS: -UNCHECKED_CAST
// FIR_IDENTICAL
// WITH_RUNTIME

fun <K, T> foo(x: (K) -> T): Pair<K, T> = (1 as K) to (1f as T)

@Target(AnnotationTarget.TYPE)
annotation class Anno

fun box(): String {
    val x = foo<@Anno Int, @Anno _> { it.toFloat() }
    return "OK"
}
