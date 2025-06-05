// ISSUE: KT-73845

// JVM_ABI_K1_K2_DIFF: K2 serializes annotation parameter default values (KT-59526).
// IGNORE_NATIVE: compatibilityTestMode=OldArtifactNewCompiler

annotation class A
annotation class B(
    val a: String = "Fail",
    val b: Array<A>
)

annotation class C(
    // `a` and `b` parameters are misplaced
    val value: B = B(b = [A()], a = "OK")
)

fun box(): String {
    val c = C()
    return c.value.a
}
