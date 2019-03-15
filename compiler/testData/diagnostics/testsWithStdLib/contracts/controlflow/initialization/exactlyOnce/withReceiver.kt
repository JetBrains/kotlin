// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

fun <T, R> T.myLet(block: (T) -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block(this)
}

fun initializationWithReceiver(y: String) {
    val x: Int
    y.myLet { x = 42 }
    x.inc()
}

fun initializationWithSafeCall(y: String?) {
    val x: Int
    y?.myLet { x = 42 }
    <!UNINITIALIZED_VARIABLE!>x<!>.inc()
}

fun sanityCheck(x: Int, y: String): Int {
    y.let { return x }
}
