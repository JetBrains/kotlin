// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'firstOrNull{}'"
// IS_APPLICABLE_2: false
fun foo(list: List<String>) {
    var result: String? = null
    <caret>for (s in list) {
        if (s.length > 0) {
            result = bar(s)
            break
        }
    }
}

fun bar(s: String): String = s
