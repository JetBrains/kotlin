// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -NOTHING_TO_INLINE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE

inline fun <T, U> Function1<T, U>.rangeTo(p: Function1<T, U>): Range<Int> {
    this..p
    p..this
    return 1..2
}

inline fun <T, U, V> ExtensionFunction1<T, U, V>.rangeTo(ext: ExtensionFunction1<T, U, V>): Range<Int> {
    ext..this
    this..ext
    return 1..2
}


inline fun <T, U, V> inlineFunWithInvoke(s: (p: T) -> U, ext: T.(p: U) -> V) {
    s..s
    s..s

    ext..ext
    ext..ext
}


fun <T, U, V> Function2<T, U, V>.rangeTo(p: Function2<T, U, V>): Range<Int> {
    return 1..2
}

fun <T, U, V, W> ExtensionFunction2<T, U, V, W>.rangeTo(ext: ExtensionFunction2<T, U, V, W>): Range<Int> {
    return 1..2
}

inline fun <T, U, V> inlineFunWithInvoke(s: (p: T, l: U) -> U, ext: T.(p: U, l: U) -> V) {
    <!USAGE_IS_NOT_INLINABLE!>s<!>..<!USAGE_IS_NOT_INLINABLE!>s<!>
    <!USAGE_IS_NOT_INLINABLE!>s<!>..<!USAGE_IS_NOT_INLINABLE!>s<!>

    <!USAGE_IS_NOT_INLINABLE!>ext<!>..<!USAGE_IS_NOT_INLINABLE!>ext<!>
    <!USAGE_IS_NOT_INLINABLE!>ext<!>..<!USAGE_IS_NOT_INLINABLE!>ext<!>
}