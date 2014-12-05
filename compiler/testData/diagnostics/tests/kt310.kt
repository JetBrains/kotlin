fun f(c: LongRange): Int {
    return c.<!FUNCTION_EXPECTED!>start<!>()
}
