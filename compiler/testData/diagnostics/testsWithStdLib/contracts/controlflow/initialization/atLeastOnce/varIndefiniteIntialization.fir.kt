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

fun <T> funWithUnknownInvocations(block: () -> T) = block()

fun indefiniteFlow() {
    var x: Int

    funWithUnknownInvocations { runTwice { x = 42 } }

    x.inc()
}

fun shadowing() {
    var x: Int
    runTwice { val x: Int; x = 42; x.inc() }
    x.inc()
}