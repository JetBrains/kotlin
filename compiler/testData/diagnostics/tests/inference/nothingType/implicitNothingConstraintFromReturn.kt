// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER

class Simple

fun test(s: Simple?): Simple? {
    myRun {
        s?.let { return it }
    }

    return s
}

inline fun <R> myRun(block: () -> R): R = TODO()
inline fun <K, V> K.let(block: (K) -> V): V = TODO()