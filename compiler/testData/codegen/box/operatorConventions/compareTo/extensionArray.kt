fun checkLess(x: Array<Int>, y: Array<Int>) = when {
    x >= y    -> "Fail $x >= $y"
    !(x < y)  -> "Fail !($x < $y)"
    !(x <= y) -> "Fail !($x <= $y)"
    x > y     -> "Fail $x > $y"
    x.compareTo(y) >= 0 -> "Fail $x.compareTo($y) >= 0"
    else -> "OK"
}

fun Array<Int>.compareTo(other: Array<Int>) = size - other.size

fun box(): String {
    val a = Array<Int>(0, {0})
    val b = Array<Int>(1, {0})
    return checkLess(a, b)
}
