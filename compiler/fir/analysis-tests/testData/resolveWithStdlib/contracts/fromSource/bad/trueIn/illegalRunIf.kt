import kotlin.contracts.*

infix fun Boolean.trueIn(target: Any)

inline fun <R> runElse(cond: Boolean, block: () -> R): R? {
    contract {
        <!POSSIBLE_FALSE_INVOCATION_CONDITION!>cond trueIn block<!>
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return if (cond) null else <!FALSE_LAMBDA_INVOCATION_CONDITION!>block()<!>
}

inline fun <R> runAlways(cond: Boolean, block: () -> R): R? {
    contract {
        <!POSSIBLE_FALSE_INVOCATION_CONDITION!>cond trueIn block<!>
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return <!FALSE_LAMBDA_INVOCATION_CONDITION!>block()<!>
}

inline fun <R> runBoth(cond: Boolean, block: () -> R): R? {
    contract {
        <!POSSIBLE_FALSE_INVOCATION_CONDITION!>cond trueIn block<!>
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return if (cond) block() else <!FALSE_LAMBDA_INVOCATION_CONDITION!>block()<!>
}
