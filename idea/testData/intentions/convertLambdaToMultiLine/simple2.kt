// WITH_RUNTIME
fun test(list: List<String>) {
    list.forEach { <caret>println(it) }
}