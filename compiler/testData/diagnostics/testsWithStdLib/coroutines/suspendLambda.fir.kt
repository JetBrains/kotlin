// SKIP_TXT
import kotlin.coroutines.*

fun <T> foo(): Continuation<T> = null!!

fun bar() {
    <!NON_MODIFIER_FORM_FOR_BUILT_IN_SUSPEND!>suspend<!> {
        println()
    }.startCoroutine(foo<Unit>())
}
