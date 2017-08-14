// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE

import kotlin.internal.*

fun <T> runTwice(@CalledInPlace(InvocationCount.AT_LEAST_ONCE) block: () -> T): T {
    block()
    return block();
};

fun testInitialization() {
    var x: Int
    <!UNINITIALIZED_VARIABLE!>x<!>.inc()
    runTwice { x = 42 }
    x.inc()
    x = 43
    x.inc()
}

fun repeatingFlow(n: Int) {
    var x: Int
    <!UNINITIALIZED_VARIABLE!>x<!>.inc()
    for (i in 1..n) {
        runTwice { x = 42 }
    }
    x.inc()
}

