// IS_APPLICABLE: false
// WITH_RUNTIME

fun plusNullable(arg: String?) = arg?.let<caret> { it + "#" } ?: ""