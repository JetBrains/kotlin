// WITH_RUNTIME
fun String.test(s: String): Boolean {
    return <caret>s.toLowerCase() == toLowerCase()
}
