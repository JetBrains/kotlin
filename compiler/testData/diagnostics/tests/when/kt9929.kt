// !WITH_NEW_INFERENCE
val test: Int = if (true) <!NI;TYPE_MISMATCH, NI;TYPE_MISMATCH!>{
    when (2) {
        1 -> 1
        else -> <!OI;NULL_FOR_NONNULL_TYPE!>null<!>
    }
}<!>
else {
    2
}