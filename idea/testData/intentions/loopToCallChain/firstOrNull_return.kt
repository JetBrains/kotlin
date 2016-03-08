// WITH_RUNTIME
fun foo(list: List<String>): String? {
    <caret>for (s in list) {
        return s
    }
    return null
}