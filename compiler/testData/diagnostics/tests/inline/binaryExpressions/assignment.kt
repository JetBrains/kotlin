// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -NOTHING_TO_INLINE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -NON_LOCAL_RETURN_NOT_ALLOWED

fun <T, U> Function1<T, U>.minusAssign(p: Function1<T, U>) {}

inline fun <T, U> Function1<T, U>.modAssign(p: Function1<T, U>) = {
    this += p
    p += this
}

inline fun <T, U> Function1<T, U>.plusAssign(p: Function1<T, U>) {
    <!USAGE_IS_NOT_INLINABLE!>this<!> -= <!USAGE_IS_NOT_INLINABLE!>p<!>
    <!USAGE_IS_NOT_INLINABLE!>p<!> -= <!USAGE_IS_NOT_INLINABLE!>this<!>
}

fun <T, U, V> ExtensionFunction1<T, U, V>.minusAssign(ext : ExtensionFunction1<T, U, V>) {}

inline fun <T, U, V> ExtensionFunction1<T, U, V>.modAssign(ext : ExtensionFunction1<T, U, V>) = {
    this += ext
    ext += this
}

inline fun <T, U, V> ExtensionFunction1<T, U, V>.plusAssign(ext : ExtensionFunction1<T, U, V>) {
    <!USAGE_IS_NOT_INLINABLE!>this<!> -= <!USAGE_IS_NOT_INLINABLE!>ext<!>
    <!USAGE_IS_NOT_INLINABLE!>ext<!> -= <!USAGE_IS_NOT_INLINABLE!>this<!>
}

inline fun <T, U, V> inlineFunWithInvoke(s: (p: T) -> U, ext: T.(p: U) -> V) {
    s += s
    ext += ext
}