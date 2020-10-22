// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: IGNORED_IN_JS
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

package demo_range

operator fun Int?.unaryPlus() : Int = this!!.unaryPlus()
operator fun Int?.dec() : Int = this!!.dec()
operator fun Int?.inc() : Int = this!!.inc()
operator fun Int?.unaryMinus() : Int = this!!.unaryMinus()

fun box() : String {
    val x : Int? = 10
    System.out?.println(x?.inv())// * x?.unaryPlus() * x?.dec() * x?.unaryMinus() as Number)
    return "OK"
}
