// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL

fun main() {
  throw <!TYPE_MISMATCH!>"str"<!>
}
