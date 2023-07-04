// FIR_IDENTICAL
// ISSUE: KT-35314

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class Foo {
    fun bar() {
        try {
            myLet {
                if (false) {
                    return
                }
            }
        } finally {
            try {
            } finally {
            }
        }
    }
}

@OptIn(ExperimentalContracts::class)
inline fun myLet(block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block()
}
