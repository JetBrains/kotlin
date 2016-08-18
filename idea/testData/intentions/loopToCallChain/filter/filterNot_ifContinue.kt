// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'filterNot{}.map{}.firstOrNull()'"
// INTENTION_TEXT_2: "Replace with 'asSequence().filterNot{}.map{}.firstOrNull()'"
fun foo(list: List<String>): Int? {
    <caret>for (s in list) {
        if (s.isEmpty()) continue
        val l = s.length
        return l
    }
    return null
}