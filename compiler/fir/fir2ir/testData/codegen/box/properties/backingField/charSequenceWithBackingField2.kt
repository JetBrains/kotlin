class Base {
    val x: CharSequence
        internal field: String = "OK"

    val s: String get() = x
}

fun box(): String {
    return Base().s
}
