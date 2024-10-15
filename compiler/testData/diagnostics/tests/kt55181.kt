// RUN_PIPELINE_TILL: SOURCE
// FIR_IDENTICAL

fun main() {
  throw <!TYPE_MISMATCH!>"str"<!>
}
