// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -NOTHING_TO_INLINE

inline fun inlineFun(s: (p: Int) -> Unit, noinline b: (p: Int) -> Unit) {
    subInline(s, b)
    subNoInline(s, b)
}

inline fun Function1<Int, Unit>.inlineExt(s: (p: Int) -> Unit, noinline b: (p: Int) -> Unit) {
    subInline(this, s, b)
    subNoInline(this, s, b)
}


inline fun subInline(vararg s: (p: Int) -> Unit) {}

fun subNoInline(vararg s: (p: Int) -> Unit) {}