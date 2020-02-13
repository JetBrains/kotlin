fun Any.withInvoke(f: String.() -> Unit) {
    if (this is String) {
        <!INAPPLICABLE_CANDIDATE!>f<!>() // Should be OK
    }
}

fun String.withInvoke(f: String.() -> Unit) {
    f()
}

