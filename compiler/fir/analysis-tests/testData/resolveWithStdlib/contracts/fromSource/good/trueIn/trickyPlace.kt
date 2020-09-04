import kotlin.contracts.*

infix fun Boolean.trueIn(target: Any)

inline fun <R> runIf1(cond: Boolean, block: () -> R): R? {
    contract {
        cond trueIn block
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    if (true) {
        if (cond) {
            if (true) {

            } else {
                return block()
            }
        }
    } else {
        return null
    }
    return null
}

inline fun <R> runIf2(cond: Boolean, block: () -> R): R? {
    contract {
        cond trueIn block
    }

    while (cond) {
        if (cond) {
            block()
        }
    }

    return null
}

inline fun <R> runIf3(cond: Boolean, block: () -> R): R? {
    contract {
        cond trueIn block
    }

    fun local() {
        if (cond) {
            block()
        }
    }

    return null
}

inline fun <R> runIf4(cond: Boolean, block: () -> R): R? {
    contract {
        cond trueIn block
    }

    class A {
        init {
            if (!cond) throw java.lang.IllegalStateException()
            block()
        }
    }

    return null
}