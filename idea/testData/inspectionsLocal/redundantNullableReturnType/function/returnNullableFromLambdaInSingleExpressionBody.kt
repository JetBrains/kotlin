// PROBLEM: none
// WITH_RUNTIME
fun test(str: String): String?<caret> = str.run {
    return null
}