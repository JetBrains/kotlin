// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE
// !LANGUAGE: +CalledInPlaceEffect

import kotlin.internal.*

fun <T> runTwice(@CalledInPlace(InvocationCount.AT_LEAST_ONCE) block: () -> T): T {
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