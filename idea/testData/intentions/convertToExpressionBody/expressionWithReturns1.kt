// IS_APPLICABLE: false

fun foo(p: Boolean): String {
    return bar() ?: return "a"
}

fun bar(): String? = null