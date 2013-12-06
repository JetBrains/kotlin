// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -NOTHING_TO_INLINE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE

inline fun <T, U> Function1<T, U>.compareTo(p: Function1<T, U>) = 1
inline fun <T, U, V> ExtensionFunction1<T, U, V>.compareTo(index : ExtensionFunction1<T, U, V>) = 1

inline fun <T, U, V> inlineFunWithInvoke(s: (p: T) -> U, ext: T.(p: U) -> V) {
    s < s
    s <= s
    s > s
    s >= s

    ext < ext
    ext > ext
    ext <= ext
    ext >= ext
}

//noinline
fun <T, U, V> Function2<T, U, V>.compareTo(index : Function2<T, U, V>) = 1
fun <T, U, V, W> ExtensionFunction2<T, U, V, W>.compareTo(index : ExtensionFunction2<T, U, V, W>) = 1

inline fun <T, U, V, W> inlineFunWithInvoke(s: (p: T, l: U) -> V, ext: T.(p: U, l: V) -> W) {
    <!USAGE_IS_NOT_INLINABLE!>s<!> < <!USAGE_IS_NOT_INLINABLE!>s<!>
    <!USAGE_IS_NOT_INLINABLE!>s<!> <= <!USAGE_IS_NOT_INLINABLE!>s<!>
    <!USAGE_IS_NOT_INLINABLE!>s<!> > <!USAGE_IS_NOT_INLINABLE!>s<!>
    <!USAGE_IS_NOT_INLINABLE!>s<!> >= <!USAGE_IS_NOT_INLINABLE!>s<!>

    <!USAGE_IS_NOT_INLINABLE!>ext<!> < <!USAGE_IS_NOT_INLINABLE!>ext<!>
    <!USAGE_IS_NOT_INLINABLE!>ext<!> > <!USAGE_IS_NOT_INLINABLE!>ext<!>
    <!USAGE_IS_NOT_INLINABLE!>ext<!> <= <!USAGE_IS_NOT_INLINABLE!>ext<!>
    <!USAGE_IS_NOT_INLINABLE!>ext<!> >= <!USAGE_IS_NOT_INLINABLE!>ext<!>
}