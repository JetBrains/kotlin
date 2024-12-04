// FIR_IDENTICAL
// SKIP_KT_DUMP

typealias AX = X

fun test() {
    X += 1
    AX += 1
}

object X {
    operator fun plusAssign(any: Any) = Unit
}
