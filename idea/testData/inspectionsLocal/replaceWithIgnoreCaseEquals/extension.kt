// WITH_RUNTIME
fun String.test(s: String): Boolean {
    return <caret>toLowerCase() == s.toLowerCase()
}
