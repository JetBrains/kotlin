// FIX: Add '.getOrThrow()' to function result (breaks use-sites!)
// WITH_RUNTIME

package kotlin

class Result<T>(val value: T?) {
    fun getOrThrow(): T = value ?: throw AssertionError("")
}

fun <caret>incorrectBlock(arg: Boolean, arg2: Boolean?): Result<Int> {
    if (arg) {
        class Local {
            fun foo(): Result<String> {
                return Result("NO")
            }
        }
        return Result(1)
    } else {
        when (arg2) {
            true -> {
                val x = fun(): Result<Boolean> {
                    return Result(false)
                }
                if (x().getOrThrow()) {
                    return Result(2)
                } else {
                    return Result(0)
                }
            }
            else -> {
                if (arg2 == false) {
                    listOf(1, 2, 3).forEach {
                        if (it == 2) return@forEach
                    }
                    return Result(3)
                }
                return Result(4)
            }
        }
    }
}
