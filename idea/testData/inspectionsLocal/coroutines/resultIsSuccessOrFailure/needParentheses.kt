// FIX: Add '.getOrThrow()' to function result (breaks use-sites!)
package kotlin

class Result<T>(val value: T?) {
    fun getOrThrow(): T = value ?: throw AssertionError("")

    operator fun plus(other: Result<T>) = other
}

fun <caret>incorrect() = Result("123") + Result("456")
