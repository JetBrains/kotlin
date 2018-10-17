package kotlin

class Result<T>(val value: T?) {
    fun getOrThrow(): T = value ?: throw AssertionError("")
}

fun test() {
    val x = <caret>fun() = Result("123")
}
