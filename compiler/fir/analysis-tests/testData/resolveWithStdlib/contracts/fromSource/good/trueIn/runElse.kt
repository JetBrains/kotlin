import kotlin.contracts.*

infix fun Boolean.trueIn(target: Any)

inline fun <R> runElse(cond: Boolean, block: () -> R): R? {
    contract {
        !cond trueIn block
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return if (cond) block() else null
}

inline fun <R> Boolean.runElse(block: () -> R): R? {
    contract {
        !this@runElse trueIn block
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return if (this) block() else null
}

fun test1(s: String?) {
    runElse(s == null) {
        s.length
    }
}

fun test2(s: String?) {
    (s == null).runElse {
        s.length
    }
}