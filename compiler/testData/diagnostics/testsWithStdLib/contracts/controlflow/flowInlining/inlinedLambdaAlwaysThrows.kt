// !LANGUAGE: +CallsInPlaceEffect
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.internal.contracts.*

inline fun myRun(block: () -> Unit): Unit {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    block()
}

fun test() {
    myRun { throw java.lang.IllegalArgumentException() }
    <!UNREACHABLE_CODE!>val x: Int = 42<!>
}