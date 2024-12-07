// RUN_PIPELINE_TILL: FRONTEND
fun test(x: Int?) {
     <!ARGUMENT_TYPE_MISMATCH!>x<!> in 1..2
}