// PROBLEM: none
// WITH_RUNTIME

fun isAlphaOrBeta(str: String) = str.let<caret> { it == "Alpha" || it == "Beta" }
