// RUN_PIPELINE_TILL: SOURCE
// FIR_IDENTICAL

fun f(c: LongRange): Int {
    return c.<!FUNCTION_EXPECTED!>start<!>()
}
