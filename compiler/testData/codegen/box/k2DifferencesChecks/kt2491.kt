// ORIGINAL: /compiler/testData/diagnostics/tests/override/kt2491.fir.kt
// WITH_STDLIB
interface T {
    public fun foo()
}

open class C {
    protected fun foo() {}
}

class D : C(), T

val obj: C = object : C(), T {}

fun box() = "OK".also { foo() }
