// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
class Foo()

fun test() {
  val f : Foo? = null
  if (f == null) {

  }
  if (f != null) {

  }
}
