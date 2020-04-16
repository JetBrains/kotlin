// WITH_RUNTIME

fun test(s: String?): String {
    return s?.let<caret> { it } ?: "Null value"
}) }