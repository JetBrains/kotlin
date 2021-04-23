// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -NOTHING_TO_INLINE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE

inline operator fun <T, U> Function1<T, U>.compareTo(p: Function1<T, U>) = 1

inline fun <T, U, V> inlineFunWithInvoke(s: (p: T) -> U) {
    s < s
    s <= s
    s > s
    s >= s
}

//noinline
operator fun <T, U, V> Function2<T, U, V>.compareTo(index : Function2<T, U, V>) = 1
operator fun <T, U, V, W> @ExtensionFunctionType Function3<T, U, V, W>.compareTo(index : @ExtensionFunctionType Function3<T, U, V, W>) = 1

inline fun <T, U, V, W> inlineFunWithInvoke(s: (p: T, l: U) -> V, ext: T.(p: U, l: V) -> W) {
    s <!USAGE_IS_NOT_INLINABLE!><<!> <!USAGE_IS_NOT_INLINABLE!>s<!>
    s <!USAGE_IS_NOT_INLINABLE!><=<!> <!USAGE_IS_NOT_INLINABLE!>s<!>
    s <!USAGE_IS_NOT_INLINABLE!>><!> <!USAGE_IS_NOT_INLINABLE!>s<!>
    s <!USAGE_IS_NOT_INLINABLE!>>=<!> <!USAGE_IS_NOT_INLINABLE!>s<!>

    ext <!USAGE_IS_NOT_INLINABLE!><<!> <!USAGE_IS_NOT_INLINABLE!>ext<!>
    ext <!USAGE_IS_NOT_INLINABLE!>><!> <!USAGE_IS_NOT_INLINABLE!>ext<!>
    ext <!USAGE_IS_NOT_INLINABLE!><=<!> <!USAGE_IS_NOT_INLINABLE!>ext<!>
    ext <!USAGE_IS_NOT_INLINABLE!>>=<!> <!USAGE_IS_NOT_INLINABLE!>ext<!>
}
