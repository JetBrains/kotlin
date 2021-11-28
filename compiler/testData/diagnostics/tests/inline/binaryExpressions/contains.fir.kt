// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -NOTHING_TO_INLINE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -RECURSION_IN_INLINE
operator inline fun <T, U> Function1<T, U>.contains(p: Function1<T, U>): Boolean {
    this in p
    p in this
    return false
}

inline fun <T, U, V> inlineFunWithInvoke(s: (p: T) -> U) {
    s in s
    s !in s
}


operator fun <T, U, V> Function2<T, U, V>.contains(p: Function2<T, U, V>): Boolean = false

operator fun <T, U, V, W> @ExtensionFunctionType Function3<T, U, V, W>.contains(ext: @ExtensionFunctionType Function3<T, U, V, W>): Boolean = false

inline fun <T, U, V> inlineFunWithInvoke(s: (p: T, l: U) -> U, ext: T.(p: U, l: U) -> V) {
    <!USAGE_IS_NOT_INLINABLE!>s<!> <!USAGE_IS_NOT_INLINABLE!>in<!> s
    <!USAGE_IS_NOT_INLINABLE!>s<!> <!USAGE_IS_NOT_INLINABLE!>!in<!> s

    <!USAGE_IS_NOT_INLINABLE!>ext<!> <!USAGE_IS_NOT_INLINABLE!>in<!> ext
    <!USAGE_IS_NOT_INLINABLE!>ext<!> <!USAGE_IS_NOT_INLINABLE!>!in<!> ext
}
