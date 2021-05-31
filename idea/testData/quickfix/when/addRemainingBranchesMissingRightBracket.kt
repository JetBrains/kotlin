// "Add remaining branches" "false"
// ACTION: Eliminate argument of 'when'
// WITH_RUNTIME

sealed class A
class B : A()

fun test(a: A) {
  val i = w<caret>hen (a) {
