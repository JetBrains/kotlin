// !DIAGNOSTICS: -UNUSED_PARAMETER -INVISIBLE_MEMBER -INVISIBLE_REFERENCE
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect

import kotlin.contracts.*

inline fun <T> myRun(block: () -> T): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block()
}

fun innerComputation(): Int = 42
fun outerComputation(): Int = 52

fun log() = Unit

fun outerFinallyInitializes() {
    val x: Int

    try {
        myRun {
            try {
                x = innerComputation()
            } catch (e: java.lang.Exception) {
                log()
            }
            // possible reassignment if innerComputation finished
            <!VAL_REASSIGNMENT!>x<!> = 42
            // x is ID here
        }

        // Definite reassignment here, cause can get here only if myRun finished
        // Not reported because of repeating diagnostic
        x = outerComputation()
    } catch (e: java.lang.Exception) {
        // can catch exception thrown by the inner, so x can be not initalized
        <!UNINITIALIZED_VARIABLE!>x<!>.inc()
        log()
    } finally {
        // Possible reassignment (e.g. if everything finished)
        // Not reported because of repeating diagnostic
        x = 42
    }

    // Properly initialized
    x.inc()
}

fun innerFinallyInitializes() {
    val x: Int
    try {
        myRun {
            try {
                innerComputation()
            } catch (e: java.lang.Exception) {
                log()
            } finally {
                x = 42
            }
        }

        // Properly initialized
        x.inc()
    } catch (e: java.lang.Exception) {
        log()
    }

    // Still can be unitialized because we don't know what can happen in try-block
    // (e.g., OutOfMemory exception could've happened even before myRun was executed)
    <!UNINITIALIZED_VARIABLE!>x<!>.inc()
}


fun innerFinallyInitializesOuterRethrows() {
    val x: Int
    try {
        myRun {
            try {
                innerComputation()
            } catch (e: java.lang.Exception) {
                log()
            } finally {
                x = 42
            }
        }

        // Properly initialized
        x.inc()
    } catch (e: java.lang.Exception) {
        log()
        throw e
    }

    // Guaranteed to be initialized because all catch-clauses are rethrowing
    x.inc()
}