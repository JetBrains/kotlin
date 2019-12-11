// KT-6822 Smart cast doesn't work inside local returned expression in lambda

val a : (Int?) -> Int = l@ {
    if (it != null) return@l it
    5
}

fun <R> let(f: (Int?) -> R): R = null!!

val b: Int = let {
    if (it != null) return@let it
    5
}

val c: Int = let {
    if (it != null) it else 5
}
