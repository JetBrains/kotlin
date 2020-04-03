// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

fun <T> myRun(block: () -> T): T {
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
            x.inc()
        }
    }
    x = 42
    x.inc()
}

fun possibleReassignmentInTryCatch() {
    val x: Int

    myRun {
        try {
            x = someComputation()
            x.inc()
        }
        catch (e: java.lang.Exception) {
            x = 42
            x.inc()
        }
        x.inc()
    }
    x.inc()
}

fun tryCatchOuter() {
    val x: Int
    try {
        myRun {  x = someComputation() }
        x.inc()
    }
    catch (e: java.lang.Exception) {
        x.inc()
    }
}
