abstract class Base(val s: String, vararg ints: Int)

fun foo(s: String, ints: IntArray) = object : Base(ints = *ints, s = s) {}

fun box(): String {
    return foo("OK", intArrayOf(1, 2)).s
}




// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: SPREAD
