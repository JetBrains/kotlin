// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

/*
 KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)

 SECTION: contracts
 CATEGORIES: analysis, controlFlow, initialization
 NUMBER: 7
 DESCRIPTION: Check initialization when type parameter of the callsInPlace is specify explicitly.
 */

// FILE: contracts.kt

package contracts

import kotlin.contracts.*

inline fun <T> case_1(block: () -> T): T {
    contract {
        callsInPlace<T>(block, InvocationKind.EXACTLY_ONCE)
    }
    return block()
}

inline fun case_2(block: () -> Int): Int {
    contract {
        callsInPlace<Number>(block, InvocationKind.EXACTLY_ONCE)
    }
    return block()
}

// FILE: usages.kt

import contracts.*

fun case_1() {
    val value_1: Int
    case_1 {
        value_1 = 10
    }
    println(value_1)
}

fun case_2() {
    val value_1: Int
    case_2 {
        value_1 = 10
        10
    }
    println(value_1)
}
