// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// FREE_COMPILER_ARGS: -Xbinary=genericSafeCasts=true
// WITH_STDLIB
@file:Suppress("UNCHECKED_CAST")

class Data(val x: Int)

fun expectCCE(block: () -> Any?): String =
    try { block(); "FAIL: no exception" } catch (e: ClassCastException) { "OK" } catch (e: Throwable) { "FAIL: $e" }

fun box() = expectCCE { var s = 0; for (d in listOf<Any>("str") as List<Data>) s += d.x; s }
