// PROBLEM: none
// WITH_RUNTIME

fun withAssign(arg: String?): String {
    var result: String = ""
    arg?.let<caret> { result = it }
    return result
}