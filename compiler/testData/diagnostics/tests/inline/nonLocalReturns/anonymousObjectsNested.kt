// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -NOTHING_TO_INLINE

inline fun <R> inlineFunOnlyLocal(crossinline p: () -> R) {
    val s = object {

        val z = p();

        init {
            doCall {
                p()
            }
        }

        fun a() {
            doCall {
                p()
            }

            p()
        }
    }
}

inline fun <R> inlineFun(p: () -> R) {
    val s = object {

        val z = <!NON_LOCAL_RETURN_NOT_ALLOWED!>p<!>()

        init {
            doCall {
                <!NON_LOCAL_RETURN_NOT_ALLOWED!>p<!>()
            }
        }


        fun a() {
            doCall {
                <!NON_LOCAL_RETURN_NOT_ALLOWED!>p<!>()
            }

            <!NON_LOCAL_RETURN_NOT_ALLOWED!>p<!>()
        }
    }
}

inline fun <R> doCall(p: () -> R) {
    p()
}
