// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -NOTHING_TO_INLINE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE
inline fun <T, V> Function1<T, V>.not() : Boolean {
    return !this
}

inline fun <T, V> ExtensionFunction1<T, T, V>.not() : Boolean {
    return !this
}

inline fun <T, V> inlineFunWithInvoke(s: (p: T) -> V, ext: T.(p: T) -> V) {
    !s
    !ext
}

fun <T, U, V> Function2<T, U, V>.not() : Boolean {
    return !this
}

fun <T, U, V, W> ExtensionFunction2<T, U, V, W>.not() : Boolean {
    return !this
}

inline fun <T, U, V> inlineFunWithInvoke(s: (p: T, l: U) -> V, ext: T.(p: T, l : U) -> V) {
    !<!USAGE_IS_NOT_INLINABLE!>s<!>
    !<!USAGE_IS_NOT_INLINABLE!>ext<!>
}