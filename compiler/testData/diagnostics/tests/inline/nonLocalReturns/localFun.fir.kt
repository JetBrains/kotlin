// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -NOTHING_TO_INLINE

inline fun <R> inlineFunOnlyLocal(crossinline p: () -> R) {
    fun a() {
        val z = p()
    }
    a()
}

inline fun <R> inlineFun(p: () -> R) {
    fun a() {
        p()
    }
    a()
}
