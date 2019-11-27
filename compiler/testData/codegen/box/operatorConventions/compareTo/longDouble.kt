// IGNORE_BACKEND_FIR: JVM_IR
fun checkLess(x: Long, y: Double) = when {
    x >= y    -> "Fail $x >= $y"
    !(x < y)  -> "Fail !($x < $y)"
    !(x <= y) -> "Fail !($x <= $y)"
    x > y     -> "Fail $x > $y"
    x.compareTo(y) >= 0 -> "Fail $x.compareTo($y) >= 0"
    else -> "OK"
}

fun box() = checkLess(0.toLong(), 0.5)
