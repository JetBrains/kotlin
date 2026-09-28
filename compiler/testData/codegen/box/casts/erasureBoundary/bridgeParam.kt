// TARGET_BACKEND: WASM
// WITH_STDLIB
@file:Suppress("UNCHECKED_CAST")

class Data(val x: Int)

fun expectCCE(block: () -> Any?): String =
    try { block(); "FAIL: no exception" } catch (e: ClassCastException) { "OK" } catch (e: Throwable) { "FAIL: $e" }

open class Base<T> { open fun f(t: T): Int = 0 }
class D : Base<Data>() { override fun f(t: Data) = t.x }
fun box() = expectCCE { (D() as Base<Any>).f("str") }
