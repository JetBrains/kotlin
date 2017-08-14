// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE -UNUSED_PARAMETER

import kotlin.internal.*

fun <T> myRun(@CalledInPlace block: () -> T): T = block()

fun functionWithSideEffects(x: Int): Int = x + 1 // ...and some other useful side-effects

fun log(s: String) = Unit // some logging or println or whatever returning Unit

fun implicitCastWithIf(s: String) {
    myRun { if (s == "") functionWithSideEffects(42) else log(s) }
}