// RUN_PIPELINE_TILL: SOURCE
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VALUE

fun foo(f: Boolean): Int {
    val i: Int
    if (f) {}
    else {
        i = 2
    }
    <!VAL_REASSIGNMENT!>i<!> = 3
    return i
}