// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -NOTHING_TO_INLINE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -VAL_REASSIGNMENT -UNUSED_CHANGED_VALUE -VARIABLE_EXPECTED

inline operator fun <T, V> Function1<T, V>.unaryPlus() = this
operator fun <T, V> Function1<T, V>.unaryMinus() = this
inline operator fun <T, V> Function1<T, V>.inc() = this
operator fun <T, V> Function1<T, V>.dec() = this

inline operator fun <T, V> @ExtensionFunctionType Function2<T, T, V>.unaryPlus(){}
operator fun <T, V> @ExtensionFunctionType Function2<T, T, V>.unaryMinus(){}
inline operator fun <T, V> @ExtensionFunctionType Function2<T, T, V>.inc() = this
operator fun <T, V> @ExtensionFunctionType Function2<T, T, V>.dec() = this

inline fun <T, V> inlineFunWithInvoke(s: (p: T) -> V, ext: T.(p: T) -> V) {
    +s
    <!USAGE_IS_NOT_INLINABLE!>-<!>s
    <!USAGE_IS_NOT_INLINABLE!>s<!>++
    <!USAGE_IS_NOT_INLINABLE!>++<!>s
    <!USAGE_IS_NOT_INLINABLE!>s<!>--
    <!USAGE_IS_NOT_INLINABLE, USAGE_IS_NOT_INLINABLE!>--<!>s
    +ext
    <!USAGE_IS_NOT_INLINABLE!>-<!>ext
    <!USAGE_IS_NOT_INLINABLE!>ext<!>++
    <!USAGE_IS_NOT_INLINABLE!>++<!>ext
    <!USAGE_IS_NOT_INLINABLE!>ext<!>--
    <!USAGE_IS_NOT_INLINABLE, USAGE_IS_NOT_INLINABLE!>--<!>ext
}

inline fun <T, V> Function1<T, V>.inlineFunWithInvoke() {
    +this
    -this
    this++
    ++this
    this--
    --this
}
