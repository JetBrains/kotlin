fun Any.withInvoke(f: String.() -> Unit) {
    if (this is String) {
        f() // Should be OK
    }
}

fun String.withInvoke(f: String.() -> Unit) {
    f()
}
