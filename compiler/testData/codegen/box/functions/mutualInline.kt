// FILE: A.kt

inline fun a(): String = b2()

// FILE: B.kt

inline fun b1(): String = a()

inline fun b2(): String = "OK"

fun box() = b1()