// LANGUAGE: +FullValueClasses
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// FREE_COMPILER_ARGS: -Xbinary=genericSafeCasts=true
// WITH_STDLIB
@file:Suppress("UNCHECKED_CAST")

class Data(val x: Int)

fun expectCCE(block: () -> Any?): String =
    try { block(); "FAIL: no exception" } catch (e: ClassCastException) { "OK" } catch (e: Throwable) { "FAIL: $e" }

value class V(val d: Data)
fun box() = expectCCE { (listOf<Any>("str") as List<V>)[0].d.x }
