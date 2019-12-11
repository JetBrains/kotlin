// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -NOTHING_TO_INLINE

inline fun <R> inlineFunOnlyLocal(crossinline p: () -> R) {
    val s = object {

        val z = p()

        fun a() {
            p()
        }
    }
}

inline fun <R> inlineFun(p: () -> R) {
    val s = object {

        val z = p()

        fun a() {
            p()
        }
    }
}
