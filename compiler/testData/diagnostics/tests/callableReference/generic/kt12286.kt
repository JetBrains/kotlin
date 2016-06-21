// !DIAGNOSTICS: -UNUSED_VARIABLE
// KT-12286 Strange type is required for generic callable reference

fun <T: Comparable<T>> maxOf(a: T, b: T): T = if (a < b) b else a

fun <T: Comparable<T>> useMaxOf() {
    val f: (T, T) -> T = ::maxOf
}
