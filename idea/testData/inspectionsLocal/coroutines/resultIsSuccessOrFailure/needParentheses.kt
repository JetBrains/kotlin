// FIX: Add '.getOrThrow()' to function result (breaks use-sites!)
package kotlin

class SuccessOrFailure<T>(val value: T?) {
    fun getOrThrow(): T = value ?: throw AssertionError("")

    operator fun plus(other: SuccessOrFailure<T>) = other
}

fun <caret>incorrect() = SuccessOrFailure("123") + SuccessOrFailure("456")
