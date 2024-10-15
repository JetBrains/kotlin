// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
fun test() = ("").<!FUNCTION_CALL_EXPECTED!>hashCode<!>::hashCode
