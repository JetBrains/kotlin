// WITH_RUNTIME
fun test(list: List<String>) {
    list.forEach { item ->
        println(item); println(item); println(item) /* comment */
    }<caret>
}