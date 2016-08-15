// <<< smartCastsWithDestructuring.txt
interface I1
interface I2

operator fun I1.component1() = 1
operator fun I2.component2() = ""

fun test(x: I1) {
    if (x !is I2) return
    val (c1, c2) = x
}