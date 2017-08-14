// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE

import kotlin.internal.*

fun <T> runTwice(@CalledInPlace(InvocationCount.AT_LEAST_ONCE) block: () -> T): T {
    block()
    return block();
};

fun <T> runOnce(@CalledInPlace(InvocationCount.EXACTLY_ONCE) block: () -> T): T {
    block()
    return block();
};

fun valueReassignment() {
    val x: Int
    <!UNINITIALIZED_VARIABLE!>x<!>.inc()
    runTwice { <!VAL_REASSIGNMENT!>x<!> = 42 }
    x.inc()
}

fun shadowing() {
    val x: Int
    runTwice { val <!NAME_SHADOWING!>x<!>: Int; x = 42; x.inc() }
    <!UNINITIALIZED_VARIABLE!>x<!>.inc()
}

fun branchingFlow(a: Any?) {
    val x: Int
    <!UNINITIALIZED_VARIABLE!>x<!>.inc()
    if (a is String) {
        runTwice { <!VAL_REASSIGNMENT!>x<!> = 42 }
    } else {
        x = 43
    }
    x.inc()
}

fun branchingFlowWithMissingBranches(a: Any?) {
    val x: Int
    if (a is String) {
        runTwice { <!VAL_REASSIGNMENT!>x<!> = 42 }
    }

    <!UNINITIALIZED_VARIABLE!>x<!>.inc()
}

fun repeatingFlow(n: Int) {
    val x: Int
    <!UNINITIALIZED_VARIABLE!>x<!>.inc()

    for (i in 1..n) {
        runTwice { <!VAL_REASSIGNMENT!>x<!> = 42 }
    }

    x.inc()
}