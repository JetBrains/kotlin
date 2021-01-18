// !WITH_NEW_INFERENCE
val test: Int = if (true) <!TYPE_MISMATCH{NI}!>{
    when (2) {
        1 -> 1
        else -> <!NULL_FOR_NONNULL_TYPE{OI}!>null<!>
    }
}<!>
else {
    2
}
