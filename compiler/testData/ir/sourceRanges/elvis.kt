fun intN(): Int? = null

fun test() = intN() ?: 1

// FIR_IDENTICAL
