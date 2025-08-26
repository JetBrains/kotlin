// TARGET_BACKEND: JVM_IR, WASM

annotation class C(val i: Int, val s: String, val arr: IntArray)

fun box(): String {
    val a = C(1, "x", intArrayOf(1,2,3))
    val b = C(i = 1, arr = intArrayOf(1,2,3), s = "x")
    if (a != b) return "Fail"
    return "OK"
}