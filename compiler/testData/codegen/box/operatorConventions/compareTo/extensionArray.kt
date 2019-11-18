// IGNORE_BACKEND_FIR: JVM_IR
fun checkLess(x: Array<Int>, y: Array<Int>) = when {
    x >= y    -> "Fail $x >= $y"
    !(x < y)  -> "Fail !($x < $y)"
    !(x <= y) -> "Fail !($x <= $y)"
    x > y     -> "Fail $x > $y"
    x.compareTo(y) >= 0 -> "Fail $x.compareTo($y) >= 0"
    else -> "OK"
}

operator fun Array<Int>.compareTo(other: Array<Int>) = size - other.size

fun box(): String {
    val a = arrayOfNulls<Int>(0) as Array<Int>
    val b = arrayOfNulls<Int>(1) as Array<Int>
    return checkLess(a, b)
}
