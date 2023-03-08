// FIR_IDENTICAL

@file:OptIn(ExperimentalContracts::class)

import kotlin.contracts.*

open class Result {
    class Success : Result()

    fun isSuccess1(arg: Result): Boolean {
        contract {
            returns(true) implies (arg is Success)
        }
        return arg is Success
    }
}

fun Result.isSuccess2(arg: Result): Boolean {
    contract {
        returns(true) implies (arg is <!UNRESOLVED_REFERENCE!>Success<!>)
    }
    return arg is <!UNRESOLVED_REFERENCE!>Success<!>
}
