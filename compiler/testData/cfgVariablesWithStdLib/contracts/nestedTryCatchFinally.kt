// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

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
            x = 42
        }

        // Definite reassignment here, cause can get here only if myRun finished
        x = outerComputation()
    } catch (e: java.lang.Exception) {
        // can catch exception thrown by the inner, so x can be not initalized
        log()
    } finally {
        // Possible reassignment (e.g. if everything finished)
        x = 42
    }

    // Properly initialized
    x.inc()
}

fun innerFinalltInitializes() {
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

    // Still can be non-initialized (e.g. if x.inc() threw an exception)
    x.inc()
}