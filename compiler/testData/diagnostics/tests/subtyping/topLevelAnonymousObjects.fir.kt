// RUN_PIPELINE_TILL: SOURCE
private var x = object {}

fun test() {
    x = <!ASSIGNMENT_TYPE_MISMATCH!>object<!> {}
}
