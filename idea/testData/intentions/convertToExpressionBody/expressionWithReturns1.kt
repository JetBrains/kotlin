// IS_APPLICABLE: false

fun foo(p: Boolean): String {
    <caret>return bar() ?: return "a"
}

fun bar(): String? = null