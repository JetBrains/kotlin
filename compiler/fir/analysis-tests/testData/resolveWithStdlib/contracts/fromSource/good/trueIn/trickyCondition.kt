import kotlin.contracts.*

infix fun Boolean.trueIn(target: Any)

inline fun <R> runIf1(cond: Boolean, block: () -> R): R? {
    contract {
        cond trueIn block
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return if (!cond) null else block()
}

inline fun <R> runIf2(cond: Boolean, block: () -> R): R? {
    contract {
        cond trueIn block
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return if (cond == true) block() else null
}

inline fun <R> runIf3(cond: Boolean, block: () -> R): R? {
    contract {
        cond trueIn block
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return if (cond != false) block() else null
}

inline fun <R> runIf4(cond: Boolean, block: () -> R): R? {
    contract {
        cond trueIn block
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return if ((cond == false) == false) block() else null
}

inline fun <R> runIf5(cond: Boolean, block: () -> R): R? {
    contract {
        cond trueIn block
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return if (cond && true) block() else null
}

inline fun <R> runIf6(cond: Boolean, block: () -> R): R? {
    contract {
        cond trueIn block
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return if (cond && cond) block() else null
}

inline fun <R> runIf7(cond: Boolean, block: () -> R): R? {
    contract {
        cond trueIn block
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }

    if(!cond) return null
    return block()
}