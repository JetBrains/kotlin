// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -NOTHING_TO_INLINE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE
inline fun inlineFunWithInvoke(s: (p: Int) -> Unit, ext: Int.(p: Int) -> Unit) {
    var d = <!USAGE_IS_NOT_INLINABLE!>s<!>
    d = <!USAGE_IS_NOT_INLINABLE!>s<!>

    var e = <!USAGE_IS_NOT_INLINABLE!>ext<!>
    e = <!USAGE_IS_NOT_INLINABLE!>ext<!>
}


inline fun Function1<Int, Unit>.inlineExt() {
    var d = this
    d = this
}
