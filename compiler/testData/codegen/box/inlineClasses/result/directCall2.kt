// WITH_STDLIB

interface I<T> {
    fun foo(x: T): Any?
}

class C : I<Result<Any?>> {
    override fun foo(x: Result<Any?>) = x.getOrNullNoinline()
}

fun <T> Result<T>.getOrNullNoinline() = getOrNull()

fun test() = C().foo(Result.success("OK"))

fun box(): String = test().toString()
