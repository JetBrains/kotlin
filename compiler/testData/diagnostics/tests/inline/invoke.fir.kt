// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -NOTHING_TO_INLINE

inline fun inlineFunWithInvoke(s: (p: Int) -> Unit, ext: Int.(p: Int) -> Unit) {
    s(11)
    s.invoke(11)
    s invoke 11

    11.ext(11)
    11 ext 11
}

inline fun inlineFunWithInvokeNonInline(noinline s: (p: Int) -> Unit, ext: Int.(p: Int) -> Unit) {
    s(11)
    s.invoke(11)
    s invoke 11

    11.ext(11)
    11 ext 11
}

inline fun Function1<Int, Unit>.inlineExt() {
    invoke(11)
    this.invoke(11)
    this invoke 11
    this(11)
}