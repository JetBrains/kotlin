// PROBLEM: none
fun test(str: String?): String? {
    val <caret>some = str ?: throw Exception()
    return when (some) {
        "some" -> some
        else -> ""
    }
}