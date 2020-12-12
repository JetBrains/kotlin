// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: IGNORED_IN_JS
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

package demo_range

operator fun Int?.rangeTo(other : Int?) : IntRange = this!!.rangeTo(other!!)

fun box() : String {
    val x : Int? = 10
    val y : Int? = 12

    for (i in x..y)
      System.out?.println(i.inv())
    return "OK"
}
