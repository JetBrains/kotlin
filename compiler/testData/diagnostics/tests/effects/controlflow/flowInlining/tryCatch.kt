// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE
// !LANGUAGE: +CalledInPlaceEffect

import kotlin.internal.*

inline fun <T> myRun(@CalledInPlace(InvocationCount.EXACTLY_ONCE) block: () -> T): T = block()

fun someComputation(): Int = 42

fun tryCatchInlined() {
    val x: Int

    myRun {
        try {
            x = someComputation()
            x.inc()
        }
        catch (e: java.lang.Exception) {
            <!UNINITIALIZED_VARIABLE!>x<!>.inc()
        }
    }
    <!VAL_REASSIGNMENT!>x<!> = 42
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
            <!VAL_REASSIGNMENT!>x<!> = 42
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
        <!UNINITIALIZED_VARIABLE!>x<!>.inc()
    }
}
