// FIR_IDENTICAL
// WITH_STDLIB
// ISSUE: KT-60312

fun test() {
    consumeVarargs(1, 2)
    consumeVarargs(arr = intArrayOf(41, 42))
}

fun consumeVarargs(vararg arr: Int) {}
