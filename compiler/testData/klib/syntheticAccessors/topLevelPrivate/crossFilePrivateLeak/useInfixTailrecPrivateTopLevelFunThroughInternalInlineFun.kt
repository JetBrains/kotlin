// FILE: A.kt
private infix tailrec fun Int.sum(that: Int): Int =
    if (this > that) 0 else that + (this sum (that - 1))

internal inline fun internalFun(x: Int) = (x - 2) sum x

// FILE: B.kt
fun box(): String =
    if (internalFun(4) == 9) "OK" else "FAIL"
