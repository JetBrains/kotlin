// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -NOTHING_TO_INLINE
import kotlin.InlineOption.*

class Z {
    inline fun <R> inlineFun(inlineOptions(ONLY_LOCAL_RETURN) p: () -> R) {
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
    Z() inlineFun @lambda {(): R ->
        return@lambda p();
    }
}