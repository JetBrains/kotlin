package kt402

fun getTypeChecker() : (Any)->Boolean {
  { a : Any -> a is <!UNRESOLVED_REFERENCE!>T<!> } // reports unsupported
}
fun f() : (Any) -> Boolean {
  return { a : Any -> a is String }
}
