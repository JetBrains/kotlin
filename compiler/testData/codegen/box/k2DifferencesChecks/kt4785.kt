// ORIGINAL: /compiler/testData/diagnostics/tests/override/kt4785.fir.kt
// WITH_STDLIB
interface T {
    fun foo()
}

open class C {
    protected fun foo() {}
}

class E : C(), T

val z: T = object : C(), T {}


fun box() = "OK".also { foo() }
