// WITH_STDLIB
// TARGET_BACKEND: JVM_IR, WASM

annotation class A(val ints: IntArray)

fun box(): String {
    val x1 = A(intArrayOf(1, 2, 3))
    val x2 = A(intArrayOf(1, 2, 3))
    val y  = A(intArrayOf(1, 3, 2))

    val ax1: Any = x1
    val ax2: Any = x2
    val ay:  Any = y

    if (!ax1.equals(ax2)) return "Fail1"
    if (ax1.hashCode() != ax2.hashCode()) return "Fail2"
    if (ax1.equals(ay)) return "Fail3"

    return "OK"
}