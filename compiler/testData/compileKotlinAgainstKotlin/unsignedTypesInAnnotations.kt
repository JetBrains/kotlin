// WITH_UNSIGNED

// FILE: A.kt

@kotlin.annotation.Target(AnnotationTarget.TYPE, AnnotationTarget.FUNCTION)
annotation class Anno(val u: UInt)

object ForTest {
    @Anno(0u)
    fun f(a: @Anno(43u) String) {}
}

// FILE: B.kt


fun box(): String {
    val result = (ForTest::f.annotations.first() as Anno).u // force annotation deserialization
    if (result != 0u) return "Fail"

    return "OK"
}