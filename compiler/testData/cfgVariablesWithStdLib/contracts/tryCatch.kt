// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

inline fun <T> myRun(block: () -> T): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block()
}

fun someComputation(): Int = 42

fun tryCatchInlined() {
    val x: Int

    myRun {
        try {
            x = someComputation()
            x.inc()
        }
        catch (e: java.lang.Exception) {
            // I?
            x.inc()
        }
    }

    // I?
    x.inc()
}

fun possibleReassignmentInTryCatch() {
    val x: Int

    myRun {
        x = 42
        try {
            x = someComputation()
            x.inc()
        }
        catch (e: java.lang.Exception) {
            // Possible reassignment
            x = 42
            x.inc()
        }
        // Initialized
        x.inc()
    }
    // Initialized
    x.inc()
}



fun tryCatchOuter() {
    var x: Int
    try {
        myRun {
            x = someComputation()
            x.inc()
        }
    }
    catch (e: java.lang.UnsupportedOperationException) {
        myRun { x = 42 }
    }
    catch (e: java.lang.Exception) {
        // do nothing
    }
    // I? because we can leave with last catch-clause which doesn't initialize x
    x.inc()
}
