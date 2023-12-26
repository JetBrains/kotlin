// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// JVM_ABI_K1_K2_DIFF: KT-63828

sealed class A : CharSequence {
    data class B(val c: CharSequence) : A(), CharSequence by c
}

fun box(): String {
    return A.B("OK").c.toString()
}
