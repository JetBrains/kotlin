// FIR_IDENTICAL
// ISSUE: KT-55552

interface B2 {
  fun d()
}

class B

open class C(b: B) : B2 by <!TYPE_MISMATCH!>b<!> {} //no error in K2, K1 - [TYPE_MISMATCH] Type mismatch: inferred type is B but B2 was expected

fun main() {
  val c = C(B()).d() //runtime AbstractMethodError
}
