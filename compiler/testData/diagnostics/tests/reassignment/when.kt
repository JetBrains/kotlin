// FIR_IDENTICAL
// LANGUAGE: +WarnAboutNonExhaustiveWhenOnAlgebraicTypes
// !DIAGNOSTICS: -UNUSED_VALUE

fun foo(f: Boolean): Int {
    val i: Int
    <!NON_EXHAUSTIVE_WHEN_STATEMENT!>when<!> (f) {
        true -> i = 1
    }
    <!VAL_REASSIGNMENT!>i<!> = 3
    return i
}
