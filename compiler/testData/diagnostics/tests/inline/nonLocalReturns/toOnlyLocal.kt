import kotlin.InlineOption.*

inline fun <R> toOnlyLocal(inlineOptions(ONLY_LOCAL_RETURN) p: () -> R) {
    p()
}

inline fun <R> inlineAll(p: () -> R) {
    toOnlyLocal(<!NON_LOCAL_RETURN_NOT_ALLOWED!>p<!>)
}
