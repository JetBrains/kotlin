// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
fun foo(a : Any) {}

fun test() {
  foo(object {});
}
