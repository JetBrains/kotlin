// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'mapIndexed{}.forEach{}'"
fun foo(list: List<String>) {
    <caret>for ((index, s) in list.withIndex()) {
        val x = s.length * index
        println(x)
    }
}