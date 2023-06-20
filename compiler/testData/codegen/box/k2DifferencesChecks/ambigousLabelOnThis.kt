// ORIGINAL: /compiler/testData/diagnostics/tests/thisAndSuper/ambigousLabelOnThis.fir.kt
// WITH_STDLIB
class Dup {
  fun Dup() : Unit {
    this@Dup
  }
}

fun box() = "OK"
