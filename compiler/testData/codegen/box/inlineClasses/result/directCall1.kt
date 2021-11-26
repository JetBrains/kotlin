// WITH_STDLIB
// IGNORE_BACKEND: JVM, WASM
interface I<T> {
    fun foo(x: T): T
}

class C : I<Result<Any?>> {
    override fun foo(x: Result<Any?>) = x
}

fun box() = C().foo(Result.success("OK")).getOrNull()
