// ORIGINAL: /compiler/testData/diagnostics/tests/multipleDefaultsNoSource.fir.kt
// WITH_STDLIB
// ISSUE: KT-61095

interface X {
    fun foo(a : Int = 1) {}
}

interface Y {
    fun foo(a : Int = 1) {}
}

object YImpl : Y

class Z1 : X, Y by YImpl {}
object Z1O : X, Y by YImpl {}


fun box() = "OK"
