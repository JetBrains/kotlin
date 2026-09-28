// TARGET_BACKEND: WASM
// WITH_STDLIB
@file:Suppress("UNCHECKED_CAST")

class Data(val x: Int)

fun expectCCE(block: () -> Any?): String =
    try { block(); "FAIL: no exception" } catch (e: ClassCastException) { "OK" } catch (e: Throwable) { "FAIL: $e" }

fun useData(d: Data) = d.x
fun box() = expectCCE { val g: (Data) -> Int = ::useData; (g as (Any) -> Int)("str") }
