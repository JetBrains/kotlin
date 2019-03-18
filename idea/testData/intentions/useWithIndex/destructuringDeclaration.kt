// WITH_RUNTIME
// IS_APPLICABLE: false
fun foo(list: List<Pair<String, String>>) {
    var index = 0
    <caret>for ((s1, s2) in list) {
        index++
    }
}