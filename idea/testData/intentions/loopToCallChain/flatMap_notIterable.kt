// WITH_RUNTIME
// IS_APPLICABLE: false
fun foo(list: List<String>): Char? {
    <caret>for (s in list) {
        for (c in s) {
            if (c != ' ') return c
        }
    }
    return null
}