package kt402

fun getTypeChecker() : (Any)->Boolean {
  <!UNUSED_FUNCTION_LITERAL!>{ (a : Any) -> a is <!UNRESOLVED_REFERENCE!>T<!> }<!> // reports unsupported
<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
fun f() : (Any) -> Boolean {
  return { (a : Any) -> a is String }
}
