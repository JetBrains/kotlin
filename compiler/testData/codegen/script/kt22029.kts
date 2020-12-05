// IGNORE_BACKEND: JVM_IR

data class Pair(val first: Int, val second: Int)

inline fun <T> run(fn: () -> T) = fn()

val fstSec = 42

val (fst, snd) = run { Pair(fstSec, fstSec) }

// expected: fst: 42