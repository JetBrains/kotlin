// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
fun foo(): Any {
    <!LOCAL_OBJECT_NOT_ALLOWED!>object Bar<!>
    return Bar
}