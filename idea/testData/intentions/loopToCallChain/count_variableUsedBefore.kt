// WITH_RUNTIME
// IS_APPLICABLE: false
// IS_APPLICABLE_2: false
fun foo(list: List<String>): Int {
    var count = 0
    <caret>for (s in list) {
        if (s.length > count) {
            count++
        }
    }
    return count
}