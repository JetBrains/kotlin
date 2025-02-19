// ISSUE: KT-73845
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD
// ^^^ KT-73845: Compiler v2.1.10 has a bug in 1st compilation phase

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
