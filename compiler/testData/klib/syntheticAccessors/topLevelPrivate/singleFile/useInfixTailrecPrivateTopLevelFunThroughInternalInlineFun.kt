private infix tailrec fun Int.gcd(other: Int): Int =
    if (other == 0) {
        this
    } else {
        other gcd (this % other)
    }

internal inline fun internalFun(x: Int) = (x - 2) gcd x

fun box(): String =
    if (internalFun(4) == 2) "OK" else "FAIL"
