// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: IGNORED_IN_JS
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

package bitwise_demo

fun Long?.shl(bits : Int?) : Long = this!!.shl(bits!!)

fun box() : String {
    val x : Long? = 10
    val y : Int? = 12

    System.out?.println(x.shl(y))
    return "OK"
}
