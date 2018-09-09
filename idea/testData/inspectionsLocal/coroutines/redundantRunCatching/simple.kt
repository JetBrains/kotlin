// WITH_RUNTIME

package kotlin

class Result<T>(val value: T?) {
    fun getOrThrow(): T = value ?: throw AssertionError("")
}

fun <T> runCatching(block: () -> T) = Result(block())

fun correct(arg: Boolean) = runCatching<caret> { if (arg) throw AssertionError("") else 12 }.getOrThrow()
