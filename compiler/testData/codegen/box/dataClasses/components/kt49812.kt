// WITH_STDLIB

data class Foo(val result: Result<Boolean>) {
    val Boolean.result: String get() = if (this) "OK" else "Fail"

    fun f(): String =
        result.getOrNull()!!.result
}

fun box(): String =
    Foo(Result.success(true)).f()
