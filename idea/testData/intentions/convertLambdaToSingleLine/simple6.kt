// WITH_RUNTIME
fun test(list: List<Pair<String, Int>>) {
    list.forEach { (s, _) ->
        println(s)
    }<caret>
}