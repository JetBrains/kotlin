package kotlin

class Result<T>(val value: T?) {
    fun getOrThrow(): T = value ?: throw AssertionError("")
}

fun test(arg: Boolean) {
    val x = foo@<caret>{
        if (!arg) {
            return@foo Result(true)
        } else {
            Result(false)
        }
    }
}
