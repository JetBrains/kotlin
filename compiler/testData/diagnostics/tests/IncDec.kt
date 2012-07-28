class IncDec() {
  fun inc() : IncDec = this
  fun dec() : IncDec = this
}

fun testIncDec() {
  var x = IncDec()
  x++
  ++x
  x--
  --x
  x = <!UNUSED_CHANGED_VALUE!>x++<!>
  x = <!UNUSED_CHANGED_VALUE!>x--<!>
  x = ++x
  x = <!UNUSED_VALUE!>--x<!>
}

class WrongIncDec() {
  fun inc() : Int = 1
  fun dec() : Int = 1
}

fun testWrongIncDec() {
  var x = WrongIncDec()
  x<!RESULT_TYPE_MISMATCH!>++<!>
  <!RESULT_TYPE_MISMATCH!>++<!>x
  x<!RESULT_TYPE_MISMATCH!>--<!>
  <!RESULT_TYPE_MISMATCH!>--<!>x
}

class UnitIncDec() {
  fun inc() : Unit {}
  fun dec() : Unit {}
}

fun testUnitIncDec() {
  var x = UnitIncDec()
  x<!INC_DEC_SHOULD_NOT_RETURN_UNIT!>++<!>
  <!INC_DEC_SHOULD_NOT_RETURN_UNIT!>++<!>x
  x<!INC_DEC_SHOULD_NOT_RETURN_UNIT!>--<!>
  <!INC_DEC_SHOULD_NOT_RETURN_UNIT!>--<!>x
  x = <!UNUSED_CHANGED_VALUE!>x<!INC_DEC_SHOULD_NOT_RETURN_UNIT!>++<!><!>
  x = <!UNUSED_CHANGED_VALUE!>x<!INC_DEC_SHOULD_NOT_RETURN_UNIT!>--<!><!>
  x = <!INC_DEC_SHOULD_NOT_RETURN_UNIT!>++<!>x
  x = <!UNUSED_VALUE!><!INC_DEC_SHOULD_NOT_RETURN_UNIT!>--<!>x<!>
}
