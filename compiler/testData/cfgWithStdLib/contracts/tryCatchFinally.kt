// LANGUAGE_VERSION: 1.3

import kotlin.internal.contracts.*

inline fun <T> myRun(block: () -> T): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block()
}

fun someComputation(): Int = 42

fun report(x: Int) = Unit

fun innerTryCatchFinally() {
    val x: Int

    myRun {
        try {
            x = someComputation()
            report(x)
        } catch (e: java.lang.Exception) {
            x = 42
            report(x)
        } finally {
            x = 0
        }
    }

    x.inc()
}