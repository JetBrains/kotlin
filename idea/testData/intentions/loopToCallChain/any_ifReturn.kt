// WITH_RUNTIME
fun foo(list: List<String>): Boolean {
    <caret>for (s in list) {
        if (s.length > 0) {
            return true
        }
    }
    return false
}