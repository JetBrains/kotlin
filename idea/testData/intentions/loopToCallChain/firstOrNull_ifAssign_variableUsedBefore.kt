// WITH_RUNTIME
// IS_APPLICABLE: false
fun foo(list: List<String>) {
    var result: String? = null
    <caret>for (s in list) {
        if (s != result) {
            result = s
            break
        }
    }
}