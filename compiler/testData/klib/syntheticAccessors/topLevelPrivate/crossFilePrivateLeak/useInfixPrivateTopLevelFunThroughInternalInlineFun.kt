// FILE: A.kt
private infix fun String.plus(that: String): String = this + that

internal inline fun internalFun() = "O" plus "K"

// FILE: B.kt
fun box(): String = internalFun()
