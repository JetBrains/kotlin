// WITH_RUNTIME

fun foo(value: Int?): Int? {
    return value<caret>?.let { it + 1 }
}