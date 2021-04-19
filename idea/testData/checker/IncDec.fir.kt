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
  <error descr="[RESULT_TYPE_MISMATCH] WrongIncDec, kotlin/Int">x++</error>
  <error descr="[RESULT_TYPE_MISMATCH] WrongIncDec, kotlin/Int">++x</error>
  <error descr="[RESULT_TYPE_MISMATCH] WrongIncDec, kotlin/Int">x--</error>
  <error descr="[RESULT_TYPE_MISMATCH] WrongIncDec, kotlin/Int">--x</error>
}

class UnitIncDec() {
  operator fun inc() : Unit {}
  operator fun dec() : Unit {}
}

fun testUnitIncDec() {
  var x = UnitIncDec()
  <error descr="[RESULT_TYPE_MISMATCH] UnitIncDec, kotlin/Unit">x++</error>
  <error descr="[RESULT_TYPE_MISMATCH] UnitIncDec, kotlin/Unit">++x</error>
  <error descr="[RESULT_TYPE_MISMATCH] UnitIncDec, kotlin/Unit">x--</error>
  <error descr="[RESULT_TYPE_MISMATCH] UnitIncDec, kotlin/Unit">--x</error>
  x = <error descr="[RESULT_TYPE_MISMATCH] UnitIncDec, kotlin/Unit">x++</error>
  x = <error descr="[RESULT_TYPE_MISMATCH] UnitIncDec, kotlin/Unit">x--</error>
  x = <error descr="[RESULT_TYPE_MISMATCH] UnitIncDec, kotlin/Unit">++x</error>
  x = <error descr="[RESULT_TYPE_MISMATCH] UnitIncDec, kotlin/Unit">--x</error>
}
