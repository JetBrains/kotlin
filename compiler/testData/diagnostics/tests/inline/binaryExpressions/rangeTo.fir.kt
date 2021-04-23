// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -NOTHING_TO_INLINE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -RECURSION_IN_INLINE

inline operator fun <T, U> Function1<T, U>.rangeTo(p: Function1<T, U>): ClosedRange<Int> {
    this..p
    p..this
    return 1..2
}

inline fun <T, U, V> inlineFunWithInvoke(s: (p: T) -> U) {
    s..s
    s..s
}


operator fun <T, U, V> Function2<T, U, V>.rangeTo(p: Function2<T, U, V>): ClosedRange<Int> {
    return 1..2
}

operator fun <T, U, V, W> @ExtensionFunctionType Function3<T, U, V, W>.rangeTo(ext: @ExtensionFunctionType Function3<T, U, V, W>): ClosedRange<Int> {
    return 1..2
}

inline fun <T, U, V> inlineFunWithInvoke(s: (p: T, l: U) -> U, ext: T.(p: U, l: U) -> V) {
    s<!USAGE_IS_NOT_INLINABLE!>..<!><!USAGE_IS_NOT_INLINABLE!>s<!>
    s<!USAGE_IS_NOT_INLINABLE!>..<!><!USAGE_IS_NOT_INLINABLE!>s<!>

    ext<!USAGE_IS_NOT_INLINABLE!>..<!><!USAGE_IS_NOT_INLINABLE!>ext<!>
    ext<!USAGE_IS_NOT_INLINABLE!>..<!><!USAGE_IS_NOT_INLINABLE!>ext<!>
}
