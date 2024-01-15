// FIR_IDENTICAL
// WITH_STDLIB
// ISSUE: KT-60312

// SEPARATE_SIGNATURE_DUMP_FOR_K2
// ^ The main function has a different mangled name when it's computed from its K1 descriptor in K/JVM.

fun main() {
    consumeVarargs(1, 2)
    consumeVarargs(arr = intArrayOf(41, 42))
}

fun consumeVarargs(vararg arr: Int) {}
