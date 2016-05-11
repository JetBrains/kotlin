// WITH_RUNTIME
// IS_APPLICABLE: false
fun foo(list: List<String>) {
    <caret>for (s in list) {
        println(s)
    }
}