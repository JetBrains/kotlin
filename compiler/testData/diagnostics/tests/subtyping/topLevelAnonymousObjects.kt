// RUN_PIPELINE_TILL: SOURCE
private var x = object {}

fun test() {
    x = <!TYPE_MISMATCH!>object<!> {}
}