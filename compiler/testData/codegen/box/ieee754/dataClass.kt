// IGNORE_BACKEND: JVM_IR
data class Test(val z1: Double, val z2: Double?)

fun box(): String {
    val x = Test(Double.NaN, Double.NaN)
    val y = Test(Double.NaN, Double.NaN)

    return if (x == y) "OK" else "fail"
}