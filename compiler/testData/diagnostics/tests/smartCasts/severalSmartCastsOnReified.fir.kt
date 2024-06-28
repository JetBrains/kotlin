// DIAGNOSTICS: -UNUSED_PARAMETER

inline fun <reified T : CharSequence?> foo(y: Any?) {
    if (y is T?) {
        if (y != null) {
            bar(y)
        }
    }
}

fun bar(x: CharSequence) {}