// TARGET_BACKEND: WASM
// WITH_STDLIB
@file:Suppress("UNCHECKED_CAST")

class Data(val x: Int)

fun expectCCE(block: () -> Any?): String =
    try { block(); "FAIL: no exception" } catch (e: ClassCastException) { "OK" } catch (e: Throwable) { "FAIL: $e" }

interface I<T> { fun f(t: T): Int }
fun box() = expectCCE { val o = object : I<Data> { override fun f(t: Data) = t.x }; (o as I<Any>).f("str") }
