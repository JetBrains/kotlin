import kotlin.contracts.*

infix fun Boolean.trueIn(target: Any)

inline fun <R> ifAnd(cond1: Boolean, cond2: Boolean, block: () -> R): R? {
    contract {
        (cond1 && cond2) trueIn block
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return if (cond1 && cond2) block() else null
}

fun test1(a: String?, b: String?, c: String?) {
    ifAnd(a != null && b != null,  c != null) {
        a.length
        b.length
        c.length
    }
}