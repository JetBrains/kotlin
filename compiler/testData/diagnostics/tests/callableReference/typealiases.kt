// !DIAGNOSTICS: -UNUSED_PARAMETER
// !WITH_NEW_INFERENCE
// Issue: KT-31199

typealias Global = String

fun String.toUpperCase(): String = TODO()

inline fun <T, R> Iterable<T>.myMap(transform: (T) -> R): List<R> {
    TODO()
}

fun usesGlobal(p: List<Global>) {
    p.myMap(Global::toUpperCase)
}