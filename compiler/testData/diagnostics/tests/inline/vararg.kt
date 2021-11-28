// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -NOTHING_TO_INLINE

inline fun inlineFun(s: (p: Int) -> Unit, noinline b: (p: Int) -> Unit) {
    subInline(<!USAGE_IS_NOT_INLINABLE!>s<!>, b)
    subNoInline(<!USAGE_IS_NOT_INLINABLE!>s<!>, b)
}

inline fun Function1<Int, Unit>.inlineExt(s: (p: Int) -> Unit, noinline b: (p: Int) -> Unit) {
    subInline(this, <!USAGE_IS_NOT_INLINABLE!>s<!>, b)
    subNoInline(this, <!USAGE_IS_NOT_INLINABLE!>s<!>, b)
}


inline fun subInline(vararg s: (p: Int) -> Unit) {}

fun subNoInline(vararg s: (p: Int) -> Unit) {}
