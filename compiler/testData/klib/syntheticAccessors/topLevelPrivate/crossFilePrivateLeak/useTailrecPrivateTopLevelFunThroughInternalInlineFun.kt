// FILE: A.kt
private tailrec fun sum(x: Int): Int =
    if (x == 0) 0 else x + sum(x - 1)

internal inline fun internalFun(x: Int) = sum(x)

// FILE: B.kt
fun box(): String =
    if (internalFun(4) == 10) "OK" else "FAIL"
