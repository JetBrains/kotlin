// !LANGUAGE: +CallsInPlaceEffect
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.internal.contracts.*

fun <T> runTwice(block: () -> T): T {
    contract {
        callsInPlace(block, InvocationKind.AT_LEAST_ONCE)
    }
    block()
    return block();
};

fun <T> funWithUnknownInvocations(block: () -> T) = block()

fun indefiniteFlow() {
    var x: Int

    funWithUnknownInvocations { runTwice { x = 42 } }

    <!UNINITIALIZED_VARIABLE!>x<!>.inc()
}

fun shadowing() {
    var x: Int
    runTwice { val <!NAME_SHADOWING!>x<!>: Int; x = 42; x.inc() }
    <!UNINITIALIZED_VARIABLE!>x<!>.inc()
}