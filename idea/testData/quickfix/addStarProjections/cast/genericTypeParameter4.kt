// "Change type arguments to <*>" "true"
fun <T> test(list: List<*>): List<T> {
    list as List<T><caret>
    return list as List<T>
}