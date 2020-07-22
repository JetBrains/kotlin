package kt402

fun getTypeChecker() : (Any)->Boolean {
  { a : Any -> a is <!OTHER_ERROR!>T<!> } // reports unsupported
}
fun f() : (Any) -> Boolean {
  return { a : Any -> a is String }
}
