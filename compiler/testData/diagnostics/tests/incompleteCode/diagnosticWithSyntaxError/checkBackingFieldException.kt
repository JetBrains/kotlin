package h

class Square() {
  var size : Double =
  <!UNRESOLVED_REFERENCE!>set<!>(<!UNRESOLVED_REFERENCE!>value<!>) {
    $area = size * size
  }

  <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>var area : Double<!>
  private set
}

fun main(args : Array<String>) {
  val s = Square()

  s.size = 2.0
}