// FIR_IDENTICAL
fun test(a: Any?) {
    if (a is String) {
        a == ""
    }
}