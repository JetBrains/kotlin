// "Change type arguments to <*>" "true"
fun <T> test6(list: List<*>) {
    val a: List<*> = list as List<T><caret>
}