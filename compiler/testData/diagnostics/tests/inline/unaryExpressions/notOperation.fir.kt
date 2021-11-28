// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -NOTHING_TO_INLINE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -RECURSION_IN_INLINE
inline operator fun <T, V> Function1<T, V>.not() : Boolean {
    return !this
}

inline fun <T, V> inlineFunWithInvoke(s: (p: T) -> V) {
    !s
}

operator fun <T, U, V> Function2<T, U, V>.not() : Boolean {
    return !this
}

operator fun <T, U, V, W> @ExtensionFunctionType Function3<T, U, V, W>.not() : Boolean {
    return !this
}

inline fun <T, U, V> inlineFunWithInvoke(s: (p: T, l: U) -> V, ext: T.(p: T, l : U) -> V) {
    <!USAGE_IS_NOT_INLINABLE!>!<!>s
    <!USAGE_IS_NOT_INLINABLE!>!<!>ext
}
