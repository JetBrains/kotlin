// TARGET_BACKEND: WASM
// ISSUE: KT-91749
// WASM_CHECK_NOT_CALLED_IN_FUNCTION: shouldNotBeCalled=getOrBoxInt inFunction=viaInterface
// WASM_CHECK_INSTRUCTION_NOT_IN_FUNCTION: instruction=struct.new inFunction=viaInterface

interface Defaults {
    fun compute(x: Int = 41, y: Int = 1): Int
}
abstract class Parent : Defaults
class Named : Parent() {
    override fun compute(x: Int, y: Int): Int = x + y
}
fun viaInterface(target: Defaults, x: Int): Int = target.compute(x)

fun box(): String {
    val named = Named()
    val anonymous = object : Parent() {
        override fun compute(x: Int, y: Int): Int = 100 + x + y
    }
    if (viaInterface(named, 0) != 1 || named.compute() != 42) return "FAIL named"
    if (viaInterface(anonymous, 0) != 101 || anonymous.compute() != 142) return "FAIL anonymous"
    if (viaInterface(named, 0) != named.compute(0, 1)) return "FAIL explicit named"
    if (viaInterface(anonymous, 0) != anonymous.compute(0, 1)) return "FAIL explicit anonymous"
    return "OK"
}
