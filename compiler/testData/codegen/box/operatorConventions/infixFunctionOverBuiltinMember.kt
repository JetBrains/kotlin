// IGNORE_BACKEND_FIR: JVM_IR
infix fun Int.rem(other: Int) = 10
infix operator fun Int.minus(other: Int): Int = 20

fun box(): String {
    val a = 5 rem 2
    if (a != 10) return "fail 1"

    val b = 5 minus 3
    if (b != 20) return "fail 2"

    val a1 = 5.rem(2)
    if (a1 != 1) return "fail 3"

    val b2 = 5.minus(3)
    if (b2 != 2) return "fail 4"

    return "OK"
}