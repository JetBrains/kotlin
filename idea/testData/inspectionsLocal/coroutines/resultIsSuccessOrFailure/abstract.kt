// FIX: Unwrap 'SuccessOrFailure' return type (breaks use-sites!)
package kotlin

class SuccessOrFailure<T>(val value: T?) {
    fun getOrThrow(): T = value ?: throw AssertionError("")
}

abstract class Abstract {
    abstract fun <caret>foo(): SuccessOrFailure<Int>
}
