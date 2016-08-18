// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'mapIndexed{}.flatMap{}.firstOrNull{}'"
// INTENTION_TEXT_2: "Replace with 'asSequence().mapIndexed{}.flatMap{}.firstOrNull{}'"
fun foo(list: List<String>): String? {
    <caret>for ((index, s) in list.withIndex()) {
        for (line in s.lines().take(index)) {
            if (line.isNotBlank()) return line
        }
    }
    return null
}