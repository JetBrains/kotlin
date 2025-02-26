// LANGUAGE: -ForbidParenthesizedLhsInAssignments
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD_1_9
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD_2_0
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD_2_1
// ^^^ Compiler v2.1.0 does not know this language feature

package name

class Test() {
  var i = 5
  val ten = 10.toLong()

  fun Long.t() = this.toInt() + i++ + ++i

  fun tt() = ten.t()
}

fun box() : String {
  var m = Test()
  return if((m.i)++ == 5 && ++(m.i) == 7 && m.tt() == 26) "OK" else "fail"
}
