// !WITH_NEW_INFERENCE
val test: Int = <!NI;TYPE_MISMATCH, NI;TYPE_MISMATCH, NI;TYPE_MISMATCH!>if (true) {
    when (2) {
        1 -> 1
        else -> <!OI;NULL_FOR_NONNULL_TYPE!>null<!>
    }
}
else {
    2
}<!>