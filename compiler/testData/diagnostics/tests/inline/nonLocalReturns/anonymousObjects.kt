// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -NOTHING_TO_INLINE
import kotlin.InlineOption.*

inline fun <R> inlineFunOnlyLocal(inlineOptions(ONLY_LOCAL_RETURN)p: () -> R) {
    val s = object {

        val z = p()

        fun a() {
            p()
        }
    }
}

inline fun <R> inlineFun(p: () -> R) {
    val s = object {

        val z = <!NON_LOCAL_RETURN_NOT_ALLOWED!>p<!>()

        fun a() {
            <!NON_LOCAL_RETURN_NOT_ALLOWED!>p<!>()
        }
    }
}