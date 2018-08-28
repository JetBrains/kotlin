// FIX: Add '.getOrThrow()' to function result (breaks use-sites!)
package kotlin

class SuccessOrFailure<T>(val value: T?) {
    fun getOrThrow(): T = value ?: throw AssertionError("")
}

fun <caret>incorrect() = SuccessOrFailure("123")
