import kotlin.InlineOption.*

inline fun <R> onlyLocal(inlineOptions(ONLY_LOCAL_RETURN)p: () -> R) {
    inlineAll(p)
}

inline fun <R> inlineAll(p: () -> R) {
    p()
}