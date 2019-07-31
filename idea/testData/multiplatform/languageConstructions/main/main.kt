@file:Suppress("UNUSED_VARIABLE")

package sample

fun testDesctructing() {
    val (x, y, t) = getCommonA()
    val xx: Int = x
    val yy: Double = y
    val tt: String = t
}

fun testIterator(): Int {
    var counter: Int = 0
    for (x in getCommonA()) {
        counter += x
    }
    return counter
}

fun testDelegate(): String = getCommonA().z