// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// ^ KT-83349 Wrong hashCode values in instantiated annotations

annotation class C(val i: Int, val s: String, val arr: IntArray)

fun box(): String {
    val a = C(1, "x", intArrayOf(1,2,3))
    val b = C(i = 1, arr = intArrayOf(1,2,3), s = "x")
    if (a != b) return "Fail"
    return "OK"
}