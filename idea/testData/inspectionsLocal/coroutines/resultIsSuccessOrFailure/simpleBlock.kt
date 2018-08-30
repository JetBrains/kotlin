// FIX: Add '.getOrThrow()' to function result (breaks use-sites!)
package kotlin

class SuccessOrFailure<T>(val value: T?) {
    fun getOrThrow(): T = value ?: throw AssertionError("")
}

fun <caret>incorrectBlock(arg: Boolean, arg2: Boolean?): SuccessOrFailure<Int> {
    if (arg) {
        return SuccessOrFailure(1)
    } else {
        when (arg2) {
            true -> return SuccessOrFailure(2)
            else -> {
                if (arg2 == false) {
                    return SuccessOrFailure(3)
                }
                return SuccessOrFailure(4)
            }
        }
    }
}
