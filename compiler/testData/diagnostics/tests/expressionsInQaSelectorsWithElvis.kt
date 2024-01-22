// FIR_IDENTICAL
// ISSUE: KT-64891
// FIR_DUMP

fun test(a: (Int.() -> Int)?, b: Int.() -> Int) {
    2.(a ?: b)()
}
