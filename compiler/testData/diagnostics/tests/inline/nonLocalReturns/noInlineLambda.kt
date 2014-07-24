// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -NOTHING_TO_INLINE
import kotlin.InlineOption.*

inline fun <R> inlineFunOnlyLocal(inlineOptions(ONLY_LOCAL_RETURN)p: () -> R) {
    {
        p()
    }()
}

inline fun <R> inlineFun(p: () -> R) {
    {
        <!NON_LOCAL_RETURN_NOT_ALLOWED!>p<!>()
    }()
}