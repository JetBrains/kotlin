// "Change type arguments to <*>" "false"
fun <T> test(list: List<*>): List<T> {
    return list as List<T><caret>
}