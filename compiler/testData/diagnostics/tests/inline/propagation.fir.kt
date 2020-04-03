// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -NOTHING_TO_INLINE -NON_LOCAL_RETURN_NOT_ALLOWED

inline fun inlineFunWithInvoke(s: (p: Int) -> Unit, ext: Int.(p: Int) -> Unit) {
    subInline(s, ext)
    subNoInline(s, ext)
}

inline fun inlineFunWithInvokeClosure(s: (p: Int) -> Unit, ext: Int.(p: Int) -> Unit) {
    subInline({p: Int -> s(p)}, { p -> this.ext(p)})
    subNoInline({p: Int -> s(p)}, { p -> this.ext(p)})
}

//No inline
inline fun inlineFunWithInvokeNonInline(noinline s: (p: Int) -> Unit, noinline ext: Int.(p: Int) -> Unit) {
    subInline(s, ext)
    subNoInline(s, ext)
}

inline fun inlineFunWithInvokeClosureNoinline(noinline s: (p: Int) -> Unit, noinline ext: Int.(p: Int) -> Unit) {
    subInline({p: Int -> s(p)}, { p -> this.ext(p)})
    subNoInline({p: Int -> s(p)}, { p -> this.ext(p)})
}

//ext function
inline fun Function1<Int, Unit>.inlineExt(ext: Int.(p: Int) -> Unit) {
    subInline(this, ext)
    subNoInline(this, ext)
}

inline fun Function1<Int, Unit>.inlineExtWithClosure(ext: Int.(p: Int) -> Unit) {
    subInline({p: Int -> this(p)}, { p -> this.ext(p)})
    subNoInline({p: Int -> this(p)}, { p -> this.ext(p)})
}

inline fun subInline(s: (p: Int) -> Unit, ext: Int.(p: Int) -> Unit) {}

fun subNoInline(s: (p: Int) -> Unit, ext: Int.(p: Int) -> Unit) {}
