// RUN_PIPELINE_TILL: SOURCE
// FIR_IDENTICAL
fun foo(): Any {
    <!LOCAL_OBJECT_NOT_ALLOWED!>object Bar<!>
    return Bar
}