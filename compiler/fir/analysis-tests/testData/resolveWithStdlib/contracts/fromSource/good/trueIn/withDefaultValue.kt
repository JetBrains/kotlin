import kotlin.contracts.*

infix fun Boolean.trueIn(target: Any)

inline fun <R> runIf(cond: Boolean, extra: Boolean = true, block: () -> R): R? {
    contract {
        (cond && extra) trueIn block
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return if (cond && extra) block() else null
}

inline fun <R> Boolean.then(extra: Boolean = true, block: () -> R): R? {
    contract {
        (this@then && extra) trueIn block
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return if (this && extra) block() else null
}

fun test1(s: String?) {
    runIf(s != null) {
        s.length
    }
}

fun test2(s: String?) {
    (s != null).then {
        s.length
    }
}