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
  operator fun inc() : Int = 1
  operator fun dec() : Int = 1
}

fun testWrongIncDec() {
  var x = WrongIncDec()
  <!RESULT_TYPE_MISMATCH!>x++<!>
  <!RESULT_TYPE_MISMATCH!>++x<!>
  <!RESULT_TYPE_MISMATCH!>x--<!>
  <!RESULT_TYPE_MISMATCH!>--x<!>
}

class UnitIncDec() {
  operator fun inc() : Unit {}
  operator fun dec() : Unit {}
}

fun testUnitIncDec() {
  var x = UnitIncDec()
  <!RESULT_TYPE_MISMATCH!>x++<!>
  <!RESULT_TYPE_MISMATCH!>++x<!>
  <!RESULT_TYPE_MISMATCH!>x--<!>
  <!RESULT_TYPE_MISMATCH!>--x<!>
  x = <!RESULT_TYPE_MISMATCH!>x++<!>
  x = <!RESULT_TYPE_MISMATCH!>x--<!>
  x = <!RESULT_TYPE_MISMATCH!>++x<!>
  x = <!RESULT_TYPE_MISMATCH!>--x<!>
}
