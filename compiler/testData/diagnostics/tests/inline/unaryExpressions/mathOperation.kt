// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -NOTHING_TO_INLINE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -VAL_REASSIGNMENT -UNUSED_CHANGED_VALUE -VARIABLE_EXPECTED

inline fun <T, V> Function1<T, V>.plus() = <!USAGE_IS_NOT_INLINABLE!>this<!>
fun <T, V> Function1<T, V>.minus() = this
inline fun <T, V> Function1<T, V>.inc() = <!USAGE_IS_NOT_INLINABLE!>this<!>
fun <T, V> Function1<T, V>.dec() = this

inline fun <T, V> @extension Function2<T, T, V>.plus(){}
fun <T, V> @extension Function2<T, T, V>.minus(){}
inline fun <T, V> @extension Function2<T, T, V>.inc() = <!USAGE_IS_NOT_INLINABLE!>this<!>
fun <T, V> @extension Function2<T, T, V>.dec() = this

inline fun <T, V> inlineFunWithInvoke(s: (p: T) -> V, ext: T.(p: T) -> V) {
    +s
    -<!USAGE_IS_NOT_INLINABLE!>s<!>
    s++
    ++s
    <!USAGE_IS_NOT_INLINABLE!>s<!>--
    --<!USAGE_IS_NOT_INLINABLE!>s<!>
    +ext
    -<!USAGE_IS_NOT_INLINABLE!>ext<!>
    ext++
    ++ext
    <!USAGE_IS_NOT_INLINABLE!>ext<!>--
    --<!USAGE_IS_NOT_INLINABLE!>ext<!>
}

inline fun <T, V> Function1<T, V>.inlineFunWithInvoke() {
    +this
    -<!USAGE_IS_NOT_INLINABLE!>this<!>
    this++
    ++this
    <!USAGE_IS_NOT_INLINABLE!>this<!>--
    --<!USAGE_IS_NOT_INLINABLE!>this<!>
}
