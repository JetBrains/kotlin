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
  x = <warning>x++</warning>
  x = <warning>x--</warning>
  x = ++x
  <warning>x =</warning> --x
}

class WrongIncDec() {
  <error>operator</error> fun inc() : Int = 1
  <error>operator</error> fun dec() : Int = 1
}

fun testWrongIncDec() {
  var x = WrongIncDec()
  x<error>++</error>
  <error>++</error>x
  x<error>--</error>
  <error>--</error>x
}

class UnitIncDec() {
  <error>operator</error> fun inc() : Unit {}
  <error>operator</error> fun dec() : Unit {}
}

fun testUnitIncDec() {
  var x = UnitIncDec()
  x<error>++</error>
  <error>++</error>x
  x<error>--</error>
  <error>--</error>x
  x = <warning>x<error>++</error></warning>
  x = <warning>x<error>--</error></warning>
  x = <error>++</error>x
  <warning>x =</warning> <error>--</error>x
}