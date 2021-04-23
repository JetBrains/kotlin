// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER -UNUSED_VARIABLE -REDUNDANT_LABEL_WARNING -UNUSED_PARAMETER -NOTHING_TO_INLINE -CAST_NEVER_SUCCEEDS
// Issues: KT-26153, KT-26191

import kotlin.contracts.*

fun foo(y: Boolean) {
    val x: Int = 42
    contract {
        returns() implies y
    }
}

inline fun case1(block: () -> Unit) {
    val contracts = listOf(
        contract {
            callsInPlace(<!USAGE_IS_NOT_INLINABLE!>block<!>, InvocationKind.EXACTLY_ONCE)
        }, contract {
            callsInPlace(<!USAGE_IS_NOT_INLINABLE!>block<!>, InvocationKind.EXACTLY_ONCE)
        }
    )
    block()
}

inline fun case_2(block: () -> Unit) = contract {
    callsInPlace(<!USAGE_IS_NOT_INLINABLE!>block<!>, InvocationKind.EXACTLY_ONCE)
}

fun case_3(block: () -> Unit) {
    class Class {
        fun innerFun(block2: () -> Unit) {
            contract {
                callsInPlace(block2, InvocationKind.EXACTLY_ONCE)
            }
            block2()
        }
    }
    return
}

inline fun case_4(number: Int?): Boolean {
    val cond = number != null
    contract {
        returns(false) implies (cond)
    } as ContractBuilder
    return number == null
}

inline fun case_5(cond: Boolean): Boolean {
    run {
        contract {
            returns(true) implies (cond)
        }
    }
    return true
}

inline fun case_6(cond: Boolean): Boolean {
    run {
        val x = 10
        contract {
            returns(true) implies (cond)
        }
    }
    return true
}

fun case_7(cond: Boolean): Boolean {
    fun innerFun() {
        contract {
            returns(true) implies (cond)
        }
    }
    return true
}
