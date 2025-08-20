private infix fun String.plus(that: String): String = this + that

internal inline fun internalFun() = "O" plus "K"

fun box(): String = internalFun()
