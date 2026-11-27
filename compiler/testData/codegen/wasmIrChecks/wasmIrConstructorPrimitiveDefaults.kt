// TARGET_BACKEND: WASM
// ISSUE: KT-91749
// WASM_CHECK_NOT_CALLED_IN_FUNCTION: shouldNotBeCalled=getOrBoxInt inFunction=makeDefaultSample
// WASM_CHECK_NOT_CALLED_IN_FUNCTION: shouldNotBeCalled=getOrBoxInt inFunction="DefaultDelegating.<init>"

open class DefaultSample(val x: Int = 41, val y: Int = x + 1)
class DefaultDelegating(x: Int) : DefaultSample(x)
fun makeDefaultSample(x: Int): DefaultSample = DefaultSample(x)

fun box(): String {
    val partial = makeDefaultSample(0)
    val direct = DefaultSample(0, 1)
    val delegated = DefaultDelegating(0)
    val omitted = DefaultSample()
    if (partial.x != direct.x || partial.y != direct.y) return "FAIL partial"
    if (delegated.x != 0 || delegated.y != 1) return "FAIL delegating"
    if (omitted.x != 41 || omitted.y != 42) return "FAIL omitted"
    return "OK"
}
