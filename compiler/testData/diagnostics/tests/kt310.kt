// FIR_IDENTICAL

fun f(c: LongRange): Int {
    return c.<!FUNCTION_EXPECTED!>start<!>()
}
