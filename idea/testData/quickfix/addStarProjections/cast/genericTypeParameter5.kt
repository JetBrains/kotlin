// "Change type arguments to <*>" "false"
fun <T> test(list: List<*>) {
    val a: List<T> = list as List<T><caret>
}