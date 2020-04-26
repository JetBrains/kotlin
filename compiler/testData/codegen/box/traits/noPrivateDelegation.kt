// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE
// DONT_TARGET_EXACT_BACKEND: WASM

package test

interface Z{

    private fun extension(): String {
        return "OK"
    }
}

object Z2 : Z {

}

fun box() : String {
    val size = Class.forName("test.Z2").declaredMethods.size
    if (size != 0) return "fail: $size"
    return "OK"
}
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: IGNORED_IN_JS
