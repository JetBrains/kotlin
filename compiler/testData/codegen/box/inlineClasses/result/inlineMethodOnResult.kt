// WITH_STDLIB
// IGNORE_BACKEND: WASM
interface I<T, V> {
    fun foo(x: T): V
}

class C : I<Result<Any?>, Any?> {
    override fun foo(x: Result<Any?>) = x.getOrNull()
}

fun box() = (C() as I<Result<Any?>, Any?>).foo(Result.success("OK"))
