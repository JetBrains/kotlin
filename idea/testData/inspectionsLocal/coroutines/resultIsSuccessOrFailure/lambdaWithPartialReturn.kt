package kotlin

class SuccessOrFailure<T>(val value: T?) {
    fun getOrThrow(): T = value ?: throw AssertionError("")
}

fun test(arg: Boolean) {
    val x = foo@<caret>{
        if (!arg) {
            return@foo SuccessOrFailure(true)
        } else {
            SuccessOrFailure(false)
        }
    }
}
