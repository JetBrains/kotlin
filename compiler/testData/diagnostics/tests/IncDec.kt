// FIR_IDENTICAL
class IncDec() {
  operator fun inc() : IncDec = this
  operator fun dec() : IncDec = this
}

fun testIncDec() {
  var x = IncDec()
  x++
  ++x
  x--
  --x
  x = x++
  x = x--
  x = ++x
  x = --x
}

class WrongIncDec() {
  <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun inc() : Int = 1
  <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun dec() : Int = 1
}

fun testWrongIncDec() {
  var x = WrongIncDec()
  x<!RESULT_TYPE_MISMATCH!>++<!>
  <!RESULT_TYPE_MISMATCH!>++<!>x
  x<!RESULT_TYPE_MISMATCH!>--<!>
  <!RESULT_TYPE_MISMATCH!>--<!>x
}

class UnitIncDec() {
  <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun inc() : Unit {}
  <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun dec() : Unit {}
}

fun testUnitIncDec() {
  var x = UnitIncDec()
  x<!INC_DEC_SHOULD_NOT_RETURN_UNIT!>++<!>
  <!INC_DEC_SHOULD_NOT_RETURN_UNIT!>++<!>x
  x<!INC_DEC_SHOULD_NOT_RETURN_UNIT!>--<!>
  <!INC_DEC_SHOULD_NOT_RETURN_UNIT!>--<!>x
  x = x<!INC_DEC_SHOULD_NOT_RETURN_UNIT!>++<!>
  x = x<!INC_DEC_SHOULD_NOT_RETURN_UNIT!>--<!>
  x = <!INC_DEC_SHOULD_NOT_RETURN_UNIT!>++<!>x
  x = <!INC_DEC_SHOULD_NOT_RETURN_UNIT!>--<!>x
}
