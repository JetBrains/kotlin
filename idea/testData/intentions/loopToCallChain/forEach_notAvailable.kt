// WITH_RUNTIME
// IS_APPLICABLE: false
// IS_APPLICABLE_2: false
fun foo(list: List<String>) {
    <caret>for (s in list) {
        println(s)
    }
}