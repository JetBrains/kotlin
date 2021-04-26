// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -NOTHING_TO_INLINE

fun <R> fun1(p: () -> R) {
    inlineFun {
        p()
    }
}

fun <R> fun1ValueArgument(p: () -> R) {
    inlineFun ({
                   p()
               })
}

fun <R> fun3(p: () -> R) {
    inlineFun {
        <!RETURN_NOT_ALLOWED!>return<!>;
    }
}

fun <R> fun3ValueArgument(p: () -> R) {
    inlineFun ({
                   <!RETURN_NOT_ALLOWED!>return<!>;
               })
}


fun <R> fun4(p: () -> R) {
    inlineFun lambda@ {
        return@lambda p();
    }
}

fun <R> fun4ValueArgument(p: () -> R) {
    inlineFun (lambda@ {
        return@lambda p();
    })
}


inline fun <R> inlineFun(crossinline p: () -> R) {
    p()
}
