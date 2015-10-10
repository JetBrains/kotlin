// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -NOTHING_TO_INLINE

class Z {
    inline infix fun <R> inlineFun(crossinline p: () -> R) {
        p()
    }
}

fun <R> fun1(p: () -> R) {
    Z() inlineFun {
        p()
    }
}

fun <R> fun3(p: () -> R) {
    Z() inlineFun {
        <!RETURN_NOT_ALLOWED!>return<!>;
    }
}

fun <R> fun4(p: () -> R) {
    Z() inlineFun lambda@ {
        return@lambda p();
    }
}
