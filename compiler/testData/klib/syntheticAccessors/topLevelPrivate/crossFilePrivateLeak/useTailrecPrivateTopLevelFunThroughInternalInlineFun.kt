// FILE: A.kt
private tailrec fun gcd(a: Int, b: Int): Int =
    if (b == 0) {
        a
    } else {
        gcd(b, a % b)
    }

internal inline fun internalFun(x: Int) = gcd(x - 2, x)

// FILE: B.kt
fun box(): String =
    if (internalFun(4) == 2) "OK" else "FAIL"
