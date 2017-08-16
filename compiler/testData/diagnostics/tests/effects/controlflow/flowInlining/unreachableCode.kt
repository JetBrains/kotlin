// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE -UNUSED_PARAMETER
// !LANGUAGE: +CalledInPlaceEffect

import kotlin.internal.*

inline fun myRun(@CalledInPlace(InvocationCount.EXACTLY_ONCE) block: () -> Unit) = block()

fun throwInLambda(): Int {
    val x = myRun { throw java.lang.IllegalArgumentException() }
    return <!TYPE_MISMATCH!>x<!>
}