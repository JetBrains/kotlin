// !WITH_NEW_INFERENCE
fun foo1(b: Boolean, c: Int) {
    if (b && c) {}
    if (b || c) {}
    if (c && b) {}
    if (c || b) {}
}
