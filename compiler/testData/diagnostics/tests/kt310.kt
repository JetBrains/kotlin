// !WITH_NEW_INFERENCE

fun f(c: LongRange): Int {
    return c.<!NI;TYPE_MISMATCH, FUNCTION_EXPECTED!>start<!>()
}
