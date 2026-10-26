// TARGET_BACKEND: WASM
// IGNORE_BACKEND: WASM_JS
// ^ value classes are boxed and their equals is called when compared with ==

value class Single(val x: Int)
value class Outer(val single: Single)

// WASM_CHECK_INSTRUCTION_NOT_IN_FUNCTION: instruction=struct.new inFunction=test
// WASM_CHECK_NOT_CALLED_IN_FUNCTION: shouldNotBeCalled=Single.equals inFunction=test
// WASM_CHECK_NOT_CALLED_IN_FUNCTION: shouldNotBeCalled=Outer.equals inFunction=test
fun test() {
    val a1 = Single(1)
    val b1 = Single(1)
    val a2 = Single(2)

    if (a1 != b1) throw AssertionError("a1 != b1")
    if (a1 == a2) throw AssertionError("a1 == a2")
    if (Outer(a1) != Outer(b1)) throw AssertionError("Outer(a1) != Outer(b1)")
    if (Outer(a1) == Outer(a2)) throw AssertionError("Outer(a1) == Outer(a2)")
}

fun box(): String {
    test()
    return "OK"
}
