// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// !USE_EXPERIMENTAL: kotlin.internal.ContractsDsl

import kotlin.contracts.*

@kotlin.contracts.ExperimentalContracts
inline fun inlineMe(block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    block()
}

@kotlin.contracts.ExperimentalContracts
inline fun crossinlineMe(crossinline block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    block()
}

@Suppress("NOTHING_TO_INLINE")
@kotlin.contracts.ExperimentalContracts
inline fun noinlineMe(noinline block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    block()
}

@kotlin.contracts.ExperimentalContracts
fun notinline(block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    block()
}

@kotlin.contracts.ExperimentalContracts
class Test {
    val a: String
    val b: String
    val c: String
    val d: String

    init {
        inlineMe {
            a = "allowed"
        }
        crossinlineMe {
            <!CAPTURED_VAL_INITIALIZATION!>b<!> = "not allowed"
        }
        noinlineMe {
            <!CAPTURED_VAL_INITIALIZATION!>c<!> = "not allowed"
        }
        notinline {
            <!CAPTURED_VAL_INITIALIZATION!>d<!> = "not allowed"
        }
    }
}
