// FIX: Rename to 'incorrectCatching'
package kotlin

class SuccessOrFailure<T>(val value: T?) {
    fun getOrThrow(): T = value ?: throw AssertionError("")
}

fun <caret>incorrect() = SuccessOrFailure("123")
