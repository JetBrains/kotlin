// RUN_PIPELINE_TILL: SOURCE
// FIR_IDENTICAL
fun test() = ("").<!FUNCTION_CALL_EXPECTED!>hashCode<!>::hashCode
