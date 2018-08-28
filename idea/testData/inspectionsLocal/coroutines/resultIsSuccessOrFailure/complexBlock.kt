// FIX: Add '.getOrThrow()' to function result (breaks use-sites!)
// WITH_RUNTIME

package kotlin

class SuccessOrFailure<T>(val value: T?) {
    fun getOrThrow(): T = value ?: throw AssertionError("")
}

fun <caret>incorrectBlock(arg: Boolean, arg2: Boolean?): SuccessOrFailure<Int> {
    if (arg) {
        class Local {
            fun foo(): SuccessOrFailure<String> {
                return SuccessOrFailure("NO")
            }
        }
        return SuccessOrFailure(1)
    } else {
        when (arg2) {
            true -> {
                val x = fun(): SuccessOrFailure<Boolean> {
                    return SuccessOrFailure(false)
                }
                if (x().getOrThrow()) {
                    return SuccessOrFailure(2)
                } else {
                    return SuccessOrFailure(0)
                }
            }
            else -> {
                if (arg2 == false) {
                    listOf(1, 2, 3).forEach {
                        if (it == 2) return@forEach
                    }
                    return SuccessOrFailure(3)
                }
                return SuccessOrFailure(4)
            }
        }
    }
}
