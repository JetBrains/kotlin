// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -NOTHING_TO_INLINE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE
operator inline fun <T, U> Function1<T, U>.get(index : Int) {

}

inline fun <T, U, V> inlineFunWithInvoke(s: (p: T) -> U) {
    s[1]
}

//noinline
operator fun <T, U, V> Function2<T, U, V>.get(index : Int) {

}

operator fun <T, U, V, W> @ExtensionFunctionType Function3<T, U, V, W>.get(index : Int) {

}

inline fun <T, U, V, W> inlineFunWithInvoke(s: (p: T, l: U) -> V, ext: T.(p: U, l: V) -> W) {
    <!USAGE_IS_NOT_INLINABLE!>s[1]<!>
    <!USAGE_IS_NOT_INLINABLE!>ext[1]<!>
}
