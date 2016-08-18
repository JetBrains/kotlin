// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'firstOrNull{}'"
// IS_APPLICABLE_2: false
fun foo(list: List<String?>) {
    var result: String? = null
    <caret>for (s in list) {
        if (s != "") {
            result = s?.substring(1)
            break
        }
    }
}
