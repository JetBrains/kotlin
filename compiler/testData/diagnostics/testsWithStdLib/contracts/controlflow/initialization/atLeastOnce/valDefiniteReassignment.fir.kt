// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

fun <T> runTwice(block: () -> T): T {
    contract {
        <!INAPPLICABLE_CANDIDATE!>callsInPlace<!>(block, InvocationKind.AT_LEAST_ONCE)
    }
    block()
    return block();
};

fun <T> runOnce(block: () -> T): T {
    contract {
        <!INAPPLICABLE_CANDIDATE!>callsInPlace<!>(block, InvocationKind.EXACTLY_ONCE)
    }
    return block();
};

fun valueReassignment() {
    val x: Int
    x.inc()
    runTwice { x = 42 }
    x.inc()
}

fun shadowing() {
    val x: Int
    runTwice { val x: Int; x = 42; x.inc() }
    x.inc()
}

fun branchingFlow(a: Any?) {
    val x: Int
    x.inc()
    if (a is String) {
        runTwice { x = 42 }
    }
    else {
        x = 43
    }
    x.inc()
}

fun branchingFlowWithMissingBranches(a: Any?) {
    val x: Int
    if (a is String) {
        runTwice { x = 42 }
    }

    x.inc()
}

fun repeatingFlow(n: Int) {
    val x: Int
    x.inc()

    for (i in 1..n) {
        runTwice { x = 42 }
    }

    x.inc()
}

fun repeatingFlow2(n: Int) {
    val x: Int

    for (i in 1..n) {
        runTwice { x = 42 }
    }

    x.inc()
}
