// WITH_RUNTIME
fun test() {
    val list = emptyList<Pair<Int, String>>()
    for (<caret>(key, value) in list)
}