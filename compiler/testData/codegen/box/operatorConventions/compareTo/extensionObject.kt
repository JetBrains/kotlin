// IGNORE_BACKEND_FIR: JVM_IR
class A(val x: Int)

operator fun A.compareTo(other: A) = x.compareTo(other.x)

fun checkLess(x: A, y: A) = when {
    x >= y    -> "Fail $x >= $y"
    !(x < y)  -> "Fail !($x < $y)"
    !(x <= y) -> "Fail !($x <= $y)"
    x > y     -> "Fail $x > $y"
    x.compareTo(y) >= 0 -> "Fail $x.compareTo($y) >= 0"
    else -> "OK"
}

fun box() = checkLess(A(0), A(1))
