// LANGUAGE: +FullValueClasses
// WITH_STDLIB
// Checks lambdas capturing an instance of a single-field full value class and of an inline value class.
// On Wasm, classes of lambdas are merged by the erased types of captured values (see `WasmClassMergingUtils.kt`):
// an inline class over a primitive gets its own class (`..._bound1_V<name>`), as its instance is captured unboxed,
// while a full value class instance is captured as any other reference (`..._bound1_A`).

value class Full(val x: Int)

inline value class Inline(val x: Int)

fun call(f: () -> Int): Int = f()

// WASM_CHECK_CALLED_IN_FUNCTION: shouldBeCalled="Function0_bound1_A.<init>" inFunction=captureFull
// WASM_CHECK_NOT_CALLED_IN_FUNCTION: shouldNotBeCalled=Function0_bound1_V inFunction=captureFull
fun captureFull(full: Full): Int = call { full.x }

// WASM_CHECK_CALLED_IN_FUNCTION: shouldBeCalled=Function0_bound1_V inFunction=captureInline
// WASM_CHECK_NOT_CALLED_IN_FUNCTION: shouldNotBeCalled="Function0_bound1_A.<init>" inFunction=captureInline
fun captureInline(inline: Inline): Int = call { inline.x }

// WASM_CHECK_CALLED_IN_FUNCTION: shouldBeCalled="Function0_bound1_A.<init>" inFunction=captureString
fun captureString(string: String): Int = call { string.length }

fun box(): String {
    if (captureFull(Full(1)) != 1) return "Fail 1"
    if (captureInline(Inline(2)) != 2) return "Fail 2"
    if (captureString("abc") != 3) return "Fail 3"
    return "OK"
}
