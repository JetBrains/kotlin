// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// !OPT_IN: kotlin.contracts.ExperimentalContracts
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

fun <T> myRun(block: () -> T): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block()
}

fun myRepeat(n: Int, action: () -> Unit) {
    contract {
        callsInPlace(action)
    }
    for (i in 1..n) action()
}

fun branchingIndetermineFlow(a: Any?) {
    val x: Int

    if (a is String) {
        myRepeat(a.length) {
            // Val reassignment because we know that repeat's lambda called in-place
            myRun { <!VAL_REASSIGNMENT!>x<!> = 42 }
        }
    }
    else {
        myRun { x = 43 }
    }

    <!UNINITIALIZED_VARIABLE!>x<!>.inc()
}

fun nonAnonymousLambdas() {
    val x: Int
    val initializer = { <!CAPTURED_VAL_INITIALIZATION!>x<!> = 42 }
    myRun(initializer)
    <!UNINITIALIZED_VARIABLE!>x<!>.inc()
}

fun multipleAssignments() {
    val x: Int
    myRepeat(42) {
        // Val reassignment because we know that repeat's lambda called in-place
        myRun { <!VAL_REASSIGNMENT!>x<!> = 42 }
    }
    <!UNINITIALIZED_VARIABLE!>x<!>.inc()
}

fun funWithUnknownInvocations(block: () -> Unit) = block()

fun nestedIndefiniteAssignment() {
    val x: Int
    // Captured val initialization reported, because we don't know anything about funWithUnknownInvocations
    funWithUnknownInvocations { myRun { <!CAPTURED_VAL_INITIALIZATION!>x<!> = 42 } }
    <!UNINITIALIZED_VARIABLE!>x<!>.inc()
}

class InitializationForbiddenInNonInitSection {
    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>val x: Int<!>

    fun setup() {
        myRun { <!VAL_REASSIGNMENT!>x<!> = 42 }
    }
}
