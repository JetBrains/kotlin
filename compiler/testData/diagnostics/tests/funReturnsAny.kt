// RUN_PIPELINE_TILL: SOURCE
// FIR_IDENTICAL
// ISSUE: KT-58460

fun someFunction() : Any {
    <!RETURN_TYPE_MISMATCH!>return<!>
}
