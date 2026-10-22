// TARGET_BACKEND: WASM
// ENABLE_TAIL_CALLS
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: Wasm-JS:2.4
// ^ `-Xwasm-enable-tail-calls` was introduced in 2.5

// Direct static tail call
// WASM_CHECK_INSTRUCTION_IN_FUNCTION: instruction=return_call inFunction=staticTailCaller

// Tail call inside an IrWhen branch
// WASM_CHECK_INSTRUCTION_IN_FUNCTION: instruction=return_call inFunction=whenTailCaller

// Tail call to a Unit-returning function
// Ignored because of KT-88927
//// WASM_CHECK_INSTRUCTION_IN_FUNCTION: instruction=return_call inFunction=unitTailCaller

// Calls inside try-catch must NOT be emitted as tail calls
// WASM_CHECK_INSTRUCTION_NOT_IN_FUNCTION: instruction=return_call inFunction=tryCatchCaller

// `tailrec` is still lowered to a loop, so neither return_call nor a recursive call appears
// WASM_CHECK_INSTRUCTION_NOT_IN_FUNCTION: instruction=return_call inFunction=tailrecCaller
// WASM_CHECK_NOT_CALLED_IN_FUNCTION: shouldNotBeCalled=tailrecCaller inFunction=tailrecCaller

// Virtual dispatch tail call should produce return_call_ref
// WASM_CHECK_INSTRUCTION_IN_FUNCTION: instruction=return_call_ref inFunction=virtualTailCaller

// Interface dispatch tail call should produce return_call_ref
// WASM_CHECK_INSTRUCTION_IN_FUNCTION: instruction=return_call_ref inFunction=interfaceTailCaller


fun staticCallee(x: Int): Int = x + 1

fun staticTailCaller(x: Int): Int = staticCallee(x)


fun whenTailCaller(x: Int): Int = when {
    x < 0 -> staticCallee(0)
    x > 100 -> staticCallee(100)
    else -> staticCallee(x)
}


fun unitCallee() {}

fun unitTailCaller() {
    return unitCallee()
}


fun tryCatchCaller(x: Int): Int {
    try {
        return staticCallee(x)
    } catch (e: Throwable) {
        return -1
    }
}


tailrec fun tailrecCaller(n: Int, acc: Int): Int =
    if (n == 0) acc else tailrecCaller(n - 1, acc + 1)


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


// Mutual recursion deep enough to overflow the JS host stack without tail-call lowering.
fun even(n: Int): Boolean = if (n == 0) true else odd(n - 1)
fun odd(n: Int): Boolean = if (n == 0) false else even(n - 1)


fun box(): String {
    if (staticTailCaller(41) != 42) return "fail static"
    if (whenTailCaller(50) != 51) return "fail when"
    unitTailCaller()
    if (tryCatchCaller(10) != 11) return "fail try-catch"
    if (tailrecCaller(50, 0) != 50) return "fail tailrec"
    if (virtualTailCaller(VirtualImpl(), 21) != 42) return "fail virtual"
    if (interfaceTailCaller(InterfaceImpl(), 35) != 42) return "fail interface"

    if (!even(100_000)) return "fail mutual recursion (even)"
    if (odd(100_000)) return "fail mutual recursion (odd)"

    return "OK"
}
