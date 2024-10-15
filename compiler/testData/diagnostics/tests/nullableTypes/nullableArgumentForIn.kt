// RUN_PIPELINE_TILL: FRONTEND
fun test(x: Int?) {
     <!TYPE_MISMATCH!>x<!> in 1..2
}