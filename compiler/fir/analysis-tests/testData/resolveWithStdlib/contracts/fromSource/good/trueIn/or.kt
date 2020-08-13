import kotlin.contracts.*

infix fun Boolean.trueIn(target: Any)

inline fun <R> ifOr(cond1: Boolean, cond2: Boolean, block: () -> R): R? {
    contract {
        (cond1 || cond2) trueIn block
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return if (cond1 || cond2) block() else null
}

fun test1(a: String?, b: String?, c: String?) {
    ifOr(a != null && b != null, b != null && c != null) {
        a.<!INAPPLICABLE_CANDIDATE!>length<!>
        b.length
        c.<!INAPPLICABLE_CANDIDATE!>length<!>
    }
}