// IGNORE_BACKEND_FIR: JVM_IR
interface A : Comparable<A>

class B(val x: Int) : A {
    override fun compareTo(other: A) = x.compareTo((other as B).x)
}

fun checkLess(x: A, y: A) = when {
    x >= y    -> "Fail $x >= $y"
    !(x < y)  -> "Fail !($x < $y)"
    !(x <= y) -> "Fail !($x <= $y)"
    x > y     -> "Fail $x > $y"
    x.compareTo(y) >= 0 -> "Fail $x.compareTo($y) >= 0"
    else -> "OK"
}

fun box() = checkLess(B(0), B(1))
