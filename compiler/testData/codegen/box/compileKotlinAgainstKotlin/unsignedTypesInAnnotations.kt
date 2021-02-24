// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: NATIVE
// WITH_RUNTIME
// WITH_STDLIB
// WITH_REFLECT

// MODULE: lib
// FILE: A.kt

@kotlin.annotation.Target(AnnotationTarget.TYPE, AnnotationTarget.FUNCTION)
annotation class Anno(val u: UInt)

const val ONE_UINT = 1u

object ForTest {
    @Anno(0u)
    fun f(a: @Anno(43u) String) {}

    @Anno(ONE_UINT)
    fun g(b: @Anno(ONE_UINT) String) {}
}

// MODULE: main(lib)
// FILE: B.kt

fun box(): String {
    val fResult = (ForTest::f.annotations.first() as Anno).u // force annotation deserialization
    if (fResult != 0u) return "Fail"

    val gResult = (ForTest::g.annotations.first() as Anno).u
    if (gResult != 1u) return "Fail"

    if (ONE_UINT != 1u) return "Fail"

    return "OK"
}
