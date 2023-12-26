// JVM_ABI_K1_K2_DIFF: KT-63960, KT-63967, KT-63963

data class Pair(val first: Int, val second: Int)

inline fun <T> run(fn: () -> T) = fn()

val fstSec = 42

val (fst, snd) = run { Pair(fstSec, fstSec) }

// expected: fst: 42