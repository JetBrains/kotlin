import kotlin.contracts.*

infix fun Boolean.trueIn(target: Any)

inline fun <R> runIf1(cond1: Boolean, cond2: Boolean, cond3: Boolean, block: () -> R): R? {
    contract {
        (cond1 && cond2 || cond3) trueIn block
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return if (cond1 || cond2) block() else null
}

inline fun <R> runIf2(cond1: Boolean, cond2: Boolean, cond3: Boolean, block: () -> R): R? {
    contract {
        (cond1 && (cond2 || cond3)) trueIn block
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return if (cond1 || cond2) block() else null
}

fun test1(a: String?, b: String?, c: String?) {
    runIf1(a != null, b != null, b != null && c != null) {
        a.<!INAPPLICABLE_CANDIDATE!>length<!>
        b.length
        c.<!INAPPLICABLE_CANDIDATE!>length<!>
    }
}

fun test2(a: String?, b: String?, c: String?) {
    runIf2(a != null, b != null, b != null && c != null) {
        a.length
        b.length
        c.<!INAPPLICABLE_CANDIDATE!>length<!>
    }
}