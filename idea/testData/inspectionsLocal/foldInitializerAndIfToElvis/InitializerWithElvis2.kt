fun foo(): Boolean {
    val v = bar() ?: return true
    <caret>if (v !is String) return false

    return true
}

fun bar(): Any? = ""