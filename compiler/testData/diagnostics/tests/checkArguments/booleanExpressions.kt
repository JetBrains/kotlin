// !WITH_NEW_INFERENCE
fun foo1(b: Boolean, c: Int) {
    if (b && <!TYPE_MISMATCH!>c<!>) {}
    if (b || <!TYPE_MISMATCH!>c<!>) {}
    if (<!TYPE_MISMATCH!>c<!> && b) {}
    if (<!TYPE_MISMATCH!>c<!> || b) {}
}
