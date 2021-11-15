// WITH_STDLIB

fun concatNonNulls(strings: List<String?>): String {
    var result = ""
    for (str in strings) {
        result += str?:continue
    }
    return result
}

fun box(): String {
    val test = concatNonNulls(listOf("abc", null, null, "", null, "def"))
    if (test != "abcdef") return "Failed: test=$test"

    return "OK"
}
