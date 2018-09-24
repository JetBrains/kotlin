// !WITH_NEW_INFERENCE
fun foo1(b: Boolean, c: Int) {
    if (b && <!NI;TYPE_MISMATCH, TYPE_MISMATCH!>c<!>) {}
    if (b || <!NI;TYPE_MISMATCH, TYPE_MISMATCH!>c<!>) {}
    if (<!NI;TYPE_MISMATCH, TYPE_MISMATCH!>c<!> && b) {}
    if (<!NI;TYPE_MISMATCH, TYPE_MISMATCH!>c<!> || b) {}
}
