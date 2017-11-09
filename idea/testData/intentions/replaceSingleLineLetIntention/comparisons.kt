// IS_APPLICABLE: false
// WITH_RUNTIME

fun isAlphaOrBeta(str: String) = str.let<caret> { it == "Alpha" || it == "Beta" }
