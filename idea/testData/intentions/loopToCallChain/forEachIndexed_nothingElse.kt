// WITH_RUNTIME
// IS_APPLICABLE: false
fun foo(list: List<String>) {
    <caret>for ((index, s) in list.withIndex()) {
        println(s.hashCode() * index)
    }
}