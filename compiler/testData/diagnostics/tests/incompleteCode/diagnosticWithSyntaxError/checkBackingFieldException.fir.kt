package h

class Square() {
  var size : Double =
  <!UNRESOLVED_REFERENCE!>set<!>(<!UNRESOLVED_REFERENCE!>value<!>) {
    //in LT this LAMBDA_EXPRESSION get parsed lazyly, but doesn't got anywhere in FIR tree (as property doesn't have place for it)
    <!SYNTAX{PSI}!>$area<!> <!SYNTAX{PSI}!>= size * size<!>
  }

  <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>var area : Double<!>
  private set
}

fun main() {
  val s = Square()

  s.size = 2.0
}
