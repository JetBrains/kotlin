// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'forEachIndexed{}'"
fun foo(list: List<String>) {
    <caret>for ((index, s) in list.withIndex()) {
        println(s.hashCode() * index)
    }
}