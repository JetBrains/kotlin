// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE

import kotlin.internal.*

fun <T> myRun(@CalledInPlace(InvocationCount.EXACTLY_ONCE) block: () -> T) = block()

fun reassignmentInUsualFlow() {
    val x: Int
    myRun { x = 42 }
    <!VAL_REASSIGNMENT!>x<!> = 43
    x.inc()
}

fun reassignment() {
    val x = <!VARIABLE_WITH_REDUNDANT_INITIALIZER!>42<!>
    myRun {
        <!VAL_REASSIGNMENT!>x<!> = 43
    }
    x.inc()
}

