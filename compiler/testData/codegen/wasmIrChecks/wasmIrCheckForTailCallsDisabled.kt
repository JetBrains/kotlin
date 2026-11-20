// TARGET_BACKEND: WASM

// Without ENABLE_TAIL_CALLS (the default), calls in tail position must be emitted as
// plain calls. Tail-call emission preserves program results, so only these
// instruction-level checks can detect an accidentally enabled default.

// Direct static tail call must stay a plain call
// WASM_CHECK_INSTRUCTION_NOT_IN_FUNCTION: instruction=return_call inFunction=staticTailCaller

// Virtual dispatch tail call must stay a plain call_ref
// WASM_CHECK_INSTRUCTION_NOT_IN_FUNCTION: instruction=return_call_ref inFunction=virtualTailCaller

// Interface dispatch tail call must stay a plain call_ref
// WASM_CHECK_INSTRUCTION_NOT_IN_FUNCTION: instruction=return_call_ref inFunction=interfaceTailCaller


fun staticCallee(x: Int): Int = x + 1

fun staticTailCaller(x: Int): Int = staticCallee(x)


abstract class VirtualBase {
    abstract fun action(x: Int): Int
}

class VirtualImpl : VirtualBase() {
    override fun action(x: Int): Int = x * 2
}

fun virtualTailCaller(b: VirtualBase, x: Int): Int = b.action(x)


interface InterfaceBase {
    fun ping(x: Int): Int
}

class InterfaceImpl : InterfaceBase {
    override fun ping(x: Int): Int = x + 7
}

fun interfaceTailCaller(b: InterfaceBase, x: Int): Int = b.ping(x)


fun box(): String {
    if (staticTailCaller(41) != 42) return "fail static"
    if (virtualTailCaller(VirtualImpl(), 21) != 42) return "fail virtual"
    if (interfaceTailCaller(InterfaceImpl(), 35) != 42) return "fail interface"

    return "OK"
}
