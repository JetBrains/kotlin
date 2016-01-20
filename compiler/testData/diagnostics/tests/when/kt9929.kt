val test: Int = if (true) {
    when (2) {
        1 -> 1
        else -> <!NULL_FOR_NONNULL_TYPE!>null<!>
    }
}
else {
    2
}