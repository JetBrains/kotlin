// TARGET_BACKEND: JVM_IR
// JVM_ABI_K1_K2_DIFF: Delegation to stdlib class annotated with @MustUseReturnValue (KT-79125)
// WITH_STDLIB

sealed class A : CharSequence {
    data class B(val c: CharSequence) : A(), CharSequence by c
}

fun box(): String {
    return A.B("OK").c.toString()
}
