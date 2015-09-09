// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -NOTHING_TO_INLINE
import kotlin.InlineOption.*

inline fun <R> inlineFunOnlyLocal(crossinline p: () -> R) {
    {
        p()
    }()
}

inline fun <R> inlineFun(p: () -> R) {
    {
        <!NON_LOCAL_RETURN_NOT_ALLOWED!>p<!>()
    }()
}
