// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

fun checkLess(x: Boolean, y: Boolean) = when {
    x >= y    -> "Fail $x >= $y"
    !(x < y)  -> "Fail !($x < $y)"
    !(x <= y) -> "Fail !($x <= $y)"
    x > y     -> "Fail $x > $y"
    x.compareTo(y) >= 0 -> "Fail $x.compareTo($y) >= 0"
    else -> "OK"
}

fun box() = checkLess(false, true)
