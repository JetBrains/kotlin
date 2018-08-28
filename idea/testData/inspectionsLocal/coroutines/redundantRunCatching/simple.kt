// WITH_RUNTIME

package kotlin

class SuccessOrFailure<T>(val value: T?) {
    fun getOrThrow(): T = value ?: throw AssertionError("")
}

fun <T> runCatching(block: () -> T) = SuccessOrFailure(block())

fun correct(arg: Boolean) = runCatching<caret> { if (arg) throw AssertionError("") else 12 }.getOrThrow()
