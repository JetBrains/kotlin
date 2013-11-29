// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE

fun Function1<Int, Unit>.noInlineExt(p: Int) {}

inline fun Function1<Int, Unit>.inlineExt2(p: Int) {
    <!USAGE_IS_NOT_INLINABLE!>noInlineExt<!>(11)
    <!USAGE_IS_NOT_INLINABLE!>this<!>.noInlineExt(11)
    <!USAGE_IS_NOT_INLINABLE!>this<!> noInlineExt 11
    <!USAGE_IS_NOT_INLINABLE!>this<!>
}

inline fun Function1<Int, Unit>.inlineExt() {
    inlineExt2(1)
    this.inlineExt2(1)
    this inlineExt2 1
}

inline fun testExtension(s: (p: Int) -> Unit) {
    s.inlineExt()
}

inline fun inlineFunWrongExtension(s: (p: Int) -> Unit) {
    <!USAGE_IS_NOT_INLINABLE!>s<!>.noInlineExt(11)
}

inline fun inlineFunNoInline(noinline s: (p: Int) -> Unit) {
    s.noInlineExt(11)
}