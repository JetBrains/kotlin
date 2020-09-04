import kotlin.contracts.*

infix fun Boolean.trueIn(target: Any)

inline fun <R> runIf1(cond: Boolean, block: () -> R): R? {
    contract {
        <!POSSIBLE_FALSE_INVOCATION_CONDITION!>cond trueIn block<!>
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return if (cond == false) <!FALSE_LAMBDA_INVOCATION_CONDITION!>block()<!> else null
}

inline fun <R> runIf2(cond: Boolean, block: () -> R): R? {
    contract {
        <!POSSIBLE_FALSE_INVOCATION_CONDITION!>cond trueIn block<!>
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return if (cond != true) <!FALSE_LAMBDA_INVOCATION_CONDITION!>block()<!> else null
}

inline fun <R> runIf3(cond: Boolean, block: () -> R): R? {
    contract {
        <!POSSIBLE_FALSE_INVOCATION_CONDITION!>cond trueIn block<!>
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return if ((cond == true) == false) <!FALSE_LAMBDA_INVOCATION_CONDITION!>block()<!> else null
}

inline fun <R> runIf4(cond: Boolean, block: () -> R): R? {
    contract {
        <!POSSIBLE_FALSE_INVOCATION_CONDITION!>cond trueIn block<!>
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return if (cond || true) <!FALSE_LAMBDA_INVOCATION_CONDITION!>block()<!> else null
}

inline fun <R> runIf5(cond: Boolean, block: () -> R): R? {
    contract {
        <!POSSIBLE_FALSE_INVOCATION_CONDITION!>cond trueIn block<!>
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return if (cond || false) <!FALSE_LAMBDA_INVOCATION_CONDITION!>block()<!> else null
}

inline fun <R> runIf6(cond: Boolean, block: () -> R): R? {
    contract {
        <!POSSIBLE_FALSE_INVOCATION_CONDITION!>cond trueIn block<!>
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }

    if(cond) return null
    return <!FALSE_LAMBDA_INVOCATION_CONDITION!>block()<!>
}