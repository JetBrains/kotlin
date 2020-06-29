fun foo(): Boolean {
    val v = bar() ?: return false
    <caret>if (v !is String) return false

    return v == ""
}

fun bar(): Any? = ""