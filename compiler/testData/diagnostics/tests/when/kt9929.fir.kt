// !WITH_NEW_INFERENCE
val test: Int = <!INITIALIZER_TYPE_MISMATCH!>if (true) {
    when (2) {
        1 -> 1
        else -> null
    }
}
else {
    2
}<!>
