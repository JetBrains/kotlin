import kotlin.InlineOption.*

inline fun <R> inlineFunWithAnnotation(inlineOptions(ONLY_LOCAL_RETURN) p: () -> R) {
    inlineFun {
        p()
    }
}

inline fun <R> inlineFun2(p: () -> R) {
    inlineFun {
        p()
    }
}

inline fun <R> inlineFun(p: () -> R) {
    p()
}