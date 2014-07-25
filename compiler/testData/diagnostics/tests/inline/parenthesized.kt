inline fun inlineFunWithInvoke(s: (p: Int) -> Unit) {
    (s)(11)
    (s).invoke(11)
    (s) invoke 11
    (<!USAGE_IS_NOT_INLINABLE, UNUSED_EXPRESSION!>s<!>)
}

inline fun Function1<Int, Unit>.inlineExt() {
    (this).invoke(11)
    (this) invoke 11
    (this)(11)
    (<!USAGE_IS_NOT_INLINABLE, UNUSED_EXPRESSION!>this<!>)
}

inline fun inlineFunWithInvoke2(s: (p: Int) -> Unit) {
    (((s)))(11)
    (((s))).invoke(11)
    (((s))) invoke 11
    (((<!USAGE_IS_NOT_INLINABLE, UNUSED_EXPRESSION!>s<!>)))
}

inline fun propagation(s: (p: Int) -> Unit) {
    inlineFunWithInvoke((@label (s: (p: Int) -> Unit)))
    inlineFunWithInvoke((@label2 @label (s: (p: Int) -> Unit)))
}