// WITH_STDLIB

interface I<T> {
    fun foo(): T
}

class C : I<Result<String>> {
    override fun foo(): Result<String> = Result.success("OK")
}

fun box(): String {
    if (((C() as I<Result<String>>).foo() as Result<String>).getOrThrow() != "OK") return "FAIL 1"
    return C().foo().getOrThrow()
}
