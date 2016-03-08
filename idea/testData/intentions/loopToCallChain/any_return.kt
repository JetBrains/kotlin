// WITH_RUNTIME
fun foo(list: List<String>): Boolean {
    <caret>for (s in list) {
        return true
    }
    return false
}