// TARGET_BACKEND: WASM

// WASM_COUNT_INSTRUCTION_IN_FUNCTION: instruction=ref.is_null inFunction=nullableCallChain count=1
// WASM_CHECK_CALLED_IN_FUNCTION: shouldBeCalled=getOrBoxInt inFunction=nullableCallChain count=1

// WASM_COUNT_INSTRUCTION_IN_FUNCTION: instruction=ref.is_null inFunction=nullableCallChainWithElvis count=1
// WASM_CHECK_CALLED_IN_FUNCTION: shouldBeCalled=getOrBoxInt inFunction=nullableCallChainWithElvis count=0

fun Any?.nullableHashCode(): Int =
    if (this === null) 0 else this.hashCode()

fun nullableCallChain(arg: Any?): Int? =
    arg?.hashCode()?.dec()?.inc()

fun nullableCallChainWithElvis(arg: Any?): Int =
    arg?.hashCode()?.dec()?.inc() ?: 42

fun box(): String {
    nullableCallChain(null)
    nullableCallChainWithElvis(null)
    return "OK"
}
