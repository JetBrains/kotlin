// WITH_RUNTIME
// IS_APPLICABLE: false
// IS_APPLICABLE_2: false
fun foo(list: List<String>): Long {
    var count = 0L
    <caret>for (s in list) {
        if (s.length > 10) {
            count++
        }
    }
    return count
}