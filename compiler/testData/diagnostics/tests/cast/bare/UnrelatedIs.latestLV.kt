// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// LATEST_LV_DIFFERENCE
interface Tr
interface G<T>

fun test(tr: Tr) = tr is <!NO_TYPE_ARGUMENTS_ON_RHS!>G<!>
