// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: BRIDGE_ISSUES
// Before we implemented native wasm strings this passed by chance, only because we inserted unbox intrinsic at the end of
// the BImpl::<get-result>. Need to find common source of this bridge problems.

interface A {
    val result: Any
}
interface B : A {
    override val result: String
}

abstract class AImpl<out Self : Any>(override val result: Self) : A
class BImpl(result: String) : AImpl<String>(result), B

fun box(): String = (BImpl("OK") as B).result
