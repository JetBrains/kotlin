// FIR_IDENTICAL
// ISSUE: KT-67281
// RUN_PIPELINE_TILL: FIR
// DIAGNOSTICS: -DEBUG_INFO_MISSING_UNRESOLVED -ERROR_SUPPRESSION

inline fun <reified T> foo(v: T) {
    // Example from KT-66005
    @Suppress("TYPE_PARAMETER_IS_NOT_AN_EXPRESSION")
    if (T == Int) println("Wat?")
}
