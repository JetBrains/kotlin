import kotlin.contracts.*

infix fun Boolean.trueIn(target: Any)

inline fun <R> ifElse(cond: Boolean, ifBlock: () -> R, thenBlock: () -> R, extra: Boolean = cond): R? {
    contract {
        (cond && extra) trueIn ifBlock
        (!cond) trueIn thenBlock
        callsInPlace(ifBlock, InvocationKind.AT_MOST_ONCE)
        callsInPlace(thenBlock, InvocationKind.AT_MOST_ONCE)
    }
    return if (cond && extra) ifBlock() else thenBlock()
}

fun test1(a: String?, b: String?) {
    ifElse(a != null || b == null, { a.<!INAPPLICABLE_CANDIDATE!>length<!> }, { b.length })
    ifElse(a != null && b == null, { a.length }, { b.<!INAPPLICABLE_CANDIDATE!>length<!> })
}