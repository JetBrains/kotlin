package h

class Square() {
  var size : Double =
  <!UNRESOLVED_REFERENCE!>set<!>(<!UNRESOLVED_REFERENCE!>value<!>) {
    <!SYNTAX!>$area<!> <!SYNTAX!>= size * size<!>
  }

  var area : Double
  private set
}

fun main() {
  val s = Square()

  s.size = 2.0
}