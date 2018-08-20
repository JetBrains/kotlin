// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

package w_range

fun box() : String {
    var i = 0
    when (i) {
        1 -> i--
        else -> { i = 2 }
    }
    System.out?.println(i)
    return "OK"
}
