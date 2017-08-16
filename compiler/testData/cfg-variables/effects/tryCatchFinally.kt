// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE
// !LANGUAGE: +CalledInPlaceEffect

import kotlin.internal.*

inline fun <T> myRun(@CalledInPlace(InvocationCount.EXACTLY_ONCE) block: () -> T): T = block()

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