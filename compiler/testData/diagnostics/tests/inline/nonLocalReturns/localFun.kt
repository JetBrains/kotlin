// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -NOTHING_TO_INLINE
import kotlin.InlineOption.*

inline fun <R> inlineFunOnlyLocal(inlineOptions(ONLY_LOCAL_RETURN)p: () -> R) {
    <!NOT_YET_SUPPORTED_IN_INLINE!>fun a() {
        val z = p()
    }<!>
    a()
}

inline fun <R> inlineFun(p: () -> R) {
    <!NOT_YET_SUPPORTED_IN_INLINE!>fun a() {
        <!NON_LOCAL_RETURN_NOT_ALLOWED!>p<!>()
    }<!>
    a()
}