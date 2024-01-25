// !CHECK_TYPE

inline fun inlineFunWithInvoke(s: (p: Int) -> Unit) {
    (s)(11)
    (s).invoke(11)
    (s) <!INFIX_MODIFIER_REQUIRED!>invoke<!> 11
    (<!USAGE_IS_NOT_INLINABLE!>s<!>)
}

<!NOTHING_TO_INLINE!>inline<!> fun Function1<Int, Unit>.inlineExt() {
    (this).invoke(11)
    (this) <!INFIX_MODIFIER_REQUIRED!>invoke<!> 11
    (this)(11)
    (this)
}

inline fun inlineFunWithInvoke2(s: (p: Int) -> Unit) {
    (((s)))(11)
    (((s))).invoke(11)
    (((s))) <!INFIX_MODIFIER_REQUIRED!>invoke<!> 11
    (((<!USAGE_IS_NOT_INLINABLE!>s<!>)))
}

inline fun propagation(s: (p: Int) -> Unit) {
    inlineFunWithInvoke((label@ s))
    inlineFunWithInvoke((<!MULTIPLE_LABELS_ARE_FORBIDDEN!>label2@<!> label@ s))
}
