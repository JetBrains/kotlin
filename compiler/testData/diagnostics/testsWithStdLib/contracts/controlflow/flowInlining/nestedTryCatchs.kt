// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

inline fun <T> myRun(block: () -> T): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block()
}

fun innerComputation(): Int = 42
fun outerComputation(): Int = 52

fun innerTryCatchInitializes() {
    val x: Int

    try {
        myRun {
            try {
                x = innerComputation()
                x.inc()
            }
            catch (e: java.lang.Exception) {
                // Potential reassignment because x.inc() could threw
                <!VAL_REASSIGNMENT!>x<!> = 42
                x.inc()
            }
        }
        // Can get here only when inlined lambda exited properly, i.e. x is initialized
        x.inc()
        outerComputation()

    } catch (e: java.lang.Exception) {
        // Can get here if innerComputation() threw an exception that wasn't catched by the inner catch (x is not initialized)
        // OR if outerComputation() threw an exception (x is initialized because we reach outer computation only when inner finished ok)
        // So, x=I? here
        <!UNINITIALIZED_VARIABLE!>x<!>.inc()

        // Potential reasignment
        x = 42
    }
    // Here x=I because outer try-catch either exited normally (x=I) or catched exception (x=I, with reassingment, though)
    x.inc()
}