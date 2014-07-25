inline fun inlineFunWithInvoke(s: (p: Int) -> Unit) {
    (s: (p: Int) -> Unit)(11)
    (s: (p: Int) -> Unit).invoke(11)
    (s: (p: Int) -> Unit) invoke 11
    (<!USAGE_IS_NOT_INLINABLE, UNUSED_EXPRESSION!>s<!>)
}

inline fun Function1<Int, Unit>.inlineExt() {
    (this: Function1<Int, Unit>).invoke(11)
    (this: Function1<Int, Unit>) invoke 11
    (this: Function1<Int, Unit>)(11)
    (<!USAGE_IS_NOT_INLINABLE!>this<!>: Function1<Int, Unit>)
}

inline fun inlineFunWithInvoke2(s: (p: Int) -> Unit) {
    (((s: (p: Int) -> Unit)))(11)
    (((s: (p: Int) -> Unit): (p: Int) -> Unit): (p: Int) -> Unit)(11)
    (((s: (p: Int) -> Unit): (p: Int) -> Unit): (p: Int) -> Unit).invoke(11)
    (((s: (p: Int) -> Unit): (p: Int) -> Unit): (p: Int) -> Unit) invoke 11
    (((<!USAGE_IS_NOT_INLINABLE!>s<!>: (p: Int) -> Unit)))
}