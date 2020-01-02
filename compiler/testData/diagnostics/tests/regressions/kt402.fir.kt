package kt402

fun getTypeChecker() : (Any)->Boolean {
  { a : Any -> a is T } // reports unsupported
}
fun f() : (Any) -> Boolean {
  return { a : Any -> a is String }
}
