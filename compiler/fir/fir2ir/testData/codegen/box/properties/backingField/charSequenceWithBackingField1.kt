class Base {
    val x: CharSequence
        internal field: String = "OK"
}

fun box(): String {
    return Base().x
}
