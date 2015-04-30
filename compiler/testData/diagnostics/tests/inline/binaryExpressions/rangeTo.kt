// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -NOTHING_TO_INLINE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -RECURSION_IN_INLINE

inline fun <T, U> Function1<T, U>.rangeTo(p: Function1<T, U>): Range<Int> {
    this..p
    p..this
    return 1..2
}

inline fun <T, U, V> inlineFunWithInvoke(s: (p: T) -> U) {
    s..s
    s..s
}


fun <T, U, V> Function2<T, U, V>.rangeTo(p: Function2<T, U, V>): Range<Int> {
    return 1..2
}

fun <T, U, V, W> @extension Function3<T, U, V, W>.rangeTo(ext: @extension Function3<T, U, V, W>): Range<Int> {
    return 1..2
}

inline fun <T, U, V> inlineFunWithInvoke(s: (p: T, l: U) -> U, ext: T.(p: U, l: U) -> V) {
    <!USAGE_IS_NOT_INLINABLE!>s<!>..<!USAGE_IS_NOT_INLINABLE!>s<!>
    <!USAGE_IS_NOT_INLINABLE!>s<!>..<!USAGE_IS_NOT_INLINABLE!>s<!>

    <!USAGE_IS_NOT_INLINABLE!>ext<!>..<!USAGE_IS_NOT_INLINABLE!>ext<!>
    <!USAGE_IS_NOT_INLINABLE!>ext<!>..<!USAGE_IS_NOT_INLINABLE!>ext<!>
}
