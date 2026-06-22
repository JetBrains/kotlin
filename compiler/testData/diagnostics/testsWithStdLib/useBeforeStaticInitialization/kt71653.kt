// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// WITH_COROUTINES

<!POSSIBLE_INITIALIZATION_DEADLOCK!>object Table1<!> {
    val reference = Table2
}

<!POSSIBLE_INITIALIZATION_DEADLOCK!>object Table2<!> {
    val reference = Table1
}
