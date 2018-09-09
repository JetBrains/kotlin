// FIX: Add '.getOrThrow()' to function result (breaks use-sites!)
package kotlin

class Result<T>(val value: T?) {
    fun getOrThrow(): T = value ?: throw AssertionError("")
}

fun <caret>incorrectBlock(arg: Boolean, arg2: Boolean?): Result<Int> {
    if (arg) {
        return Result(1)
    } else {
        when (arg2) {
            true -> return Result(2)
            else -> {
                if (arg2 == false) {
                    return Result(3)
                }
                return Result(4)
            }
        }
    }
}
