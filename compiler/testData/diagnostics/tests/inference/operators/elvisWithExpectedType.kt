// FIR_IDENTICAL
// WITH_STDLIB
// ISSUE: KT-72996

class Lambda {
    fun invalidate(reason: Any?) {}
    fun invalidate() {}
}

fun test1(lambda: Lambda?): (Any?) -> Unit =
    lambda?.let {
        it::invalidate
    } ?: {}

fun test2(q: ((Any?) -> Unit)?, lambda: Lambda): (Any?) -> Unit =
    q ?: run { lambda::invalidate }
