// ORIGINAL: /compiler/testData/diagnostics/tests/override/MultipleDefaultParametersInSupertypes.fir.kt
// WITH_STDLIB
interface X {
    fun foo(a : Int = 1)
}

interface Y {
    fun foo(a : Int = 1)
}

class Z : X, Y {
    override fun foo(a : Int) {}
}

object ZO : X, Y {
    override fun foo(a : Int) {}
}

fun box() = "OK"
