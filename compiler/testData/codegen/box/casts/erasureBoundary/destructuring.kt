// TARGET_BACKEND: WASM
// WITH_STDLIB
// Heap pollution must be detected where a value produced at an erased generic type is narrowed back
// to a substituted type (an erasure boundary): with a ClassCastException, not a Wasm trap.
// The result of an erased generic `componentN` is narrowed to the substituted type.
@file:Suppress("UNCHECKED_CAST")

class Data(val x: Int)

fun expectCCE(block: () -> Any?): String =
    try { block(); "FAIL: no exception" } catch (e: ClassCastException) { "OK" } catch (e: Throwable) { "FAIL: $e" }

fun box() = expectCCE { val (a, b) = Pair<Any, Int>("str", 1) as Pair<Data, Int>; a.x + b }
