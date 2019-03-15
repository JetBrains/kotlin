// !WITH_NEW_INFERENCE

fun f(c: LongRange): Int {
    return c.<!FUNCTION_EXPECTED, NI;TYPE_MISMATCH!>start<!>()
}
