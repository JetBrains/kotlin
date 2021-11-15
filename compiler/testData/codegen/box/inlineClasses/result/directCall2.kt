// WITH_STDLIB
// IGNORE_BACKEND: JVM, WASM
interface I<T> {
    fun foo(x: T): Any?
}

class C : I<Result<Any?>> {
    override fun foo(x: Result<Any?>) = x.getOrNullNoinline()
}

fun <T> Result<T>.getOrNullNoinline() = getOrNull()

fun box() = C().foo(Result.success("OK"))
