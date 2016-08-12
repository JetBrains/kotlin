// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'toMutableSet()'"
// IS_APPLICABLE_2: false
fun foo(map: Map<Int, String>): MutableCollection<String> {
    val result = hashSetOf<String>()
    <caret>for (s in map.values) {
        result.add(s)
    }
    return result
}