// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'filterNotNull().map{}.firstOrNull()'"
// INTENTION_TEXT_2: "Replace with 'asSequence().filterNotNull().map{}.firstOrNull()'"
fun foo(list: List<String?>): Int? {
    <caret>for (s in list) {
        if (s != null) {
            val length = s.length
            return length
        }
    }
    return null
}