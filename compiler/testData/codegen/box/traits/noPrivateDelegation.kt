// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: IGNORED_IN_JS
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

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