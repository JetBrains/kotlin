// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -NOTHING_TO_INLINE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -CONFLICTING_JVM_DECLARATIONS -CONFLICTING_OVERLOADS
operator fun <T, U> Function1<T, U>.minus(p: Function1<T, U>) {

}

operator fun <T, U, V> @ExtensionFunctionType Function2<T, U, V>.minus(p: T.(p: U) -> V) {

}

inline operator fun <T, U> Function1<T, U>.plus(p: Function1<T, U>) {
    this - <!USAGE_IS_NOT_INLINABLE!>p<!>
}

inline operator fun <T, U, V> @ExtensionFunctionType Function2<T, U, V>.plus(p: T.(p: U) -> V) {
    this - <!USAGE_IS_NOT_INLINABLE!>p<!>
}

inline fun <T, U, V> inlineFunWithInvoke(s: (p: T) -> U, ext: T.(p: U) -> V) {
    s + s
    ext + ext
}

inline fun <T, U, V> inlineFunWithInvoke(s: (p: T) -> U, ext: T.(p: U) -> V) {
    s + s
    ext + ext
}

inline fun <T, U> Function1<T, U>.submit() {
    this + this
}

inline fun <T, U, V> @ExtensionFunctionType Function2<T, U, V>.submit() {
    this + this
}
