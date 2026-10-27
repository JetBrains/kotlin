// TARGET_BACKEND: WASM
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: Wasm-JS:2.4
// ^ `-Xwasm-enable-tail-calls` was introduced in 2.5

// Calls to imported functions in tail position must stay plain calls: V8 before 15.1 skips its
// well-known-import fast path for `return_call` (https://crbug.com/527088951).

// @JsFun import
// WASM_CHECK_INSTRUCTION_NOT_IN_FUNCTION: instruction=return_call inFunction=jsFunTailCaller
// WASM_CHECK_CALLED_IN_FUNCTION: shouldBeCalled=jsIncrement inFunction=jsFunTailCaller

// Same shape with a Kotlin callee is still a tail call
// WASM_CHECK_INSTRUCTION_IN_FUNCTION: instruction=return_call inFunction=kotlinTailCaller

@JsFun("(x) => x + 1")
external fun jsIncrement(x: Int): Int

fun jsFunTailCaller(x: Int): Int = jsIncrement(x)


fun kotlinIncrement(x: Int): Int = x + 1

fun kotlinTailCaller(x: Int): Int = kotlinIncrement(x)


fun box(): String {
    if (jsFunTailCaller(41) != 42) return "fail jsFun"
    if (kotlinTailCaller(41) != 42) return "fail kotlin"
    return "OK"
}
