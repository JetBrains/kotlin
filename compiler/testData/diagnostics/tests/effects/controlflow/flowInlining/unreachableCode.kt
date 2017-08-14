// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE -UNUSED_PARAMETER

import kotlin.internal.*

inline fun myRun(@CalledInPlace(InvocationCount.EXACTLY_ONCE) block: () -> Unit) = block()

fun throwInLambda(): Int {
    <!UNREACHABLE_CODE!>val <!UNUSED_VARIABLE!>x<!> =<!> myRun { throw java.lang.IllegalArgumentException() }
    <!UNREACHABLE_CODE!>return x<!>
}