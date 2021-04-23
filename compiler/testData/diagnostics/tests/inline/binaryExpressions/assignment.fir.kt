// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -NOTHING_TO_INLINE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -NON_LOCAL_RETURN_NOT_ALLOWED

operator fun <T, U> Function1<T, U>.minusAssign(p: Function1<T, U>) {}

inline operator fun <T, U> Function1<T, U>.modAssign(p: Function1<T, U>) = {
    this += p
    p += this
}

inline operator fun <T, U> Function1<T, U>.plusAssign(p: Function1<T, U>) {
    this -= <!USAGE_IS_NOT_INLINABLE!>p<!>
    p <!USAGE_IS_NOT_INLINABLE!>-=<!> this
}

operator fun <T, U, V> @ExtensionFunctionType Function2<T, U, V>.minusAssign(ext : @ExtensionFunctionType Function2<T, U, V>) {}

inline operator fun <T, U, V> @ExtensionFunctionType Function2<T, U, V>.modAssign(ext : @ExtensionFunctionType Function2<T, U, V>) = {
    this += ext
    ext += this
}

inline operator fun <T, U, V> @ExtensionFunctionType Function2<T, U, V>.plusAssign(ext : @ExtensionFunctionType Function2<T, U, V>) {
    this -= <!USAGE_IS_NOT_INLINABLE!>ext<!>
    ext <!USAGE_IS_NOT_INLINABLE!>-=<!> this
}

inline fun <T, U, V> inlineFunWithInvoke(s: (p: T) -> U, ext: T.(p: U) -> V) {
    s += s
    ext += ext
}
