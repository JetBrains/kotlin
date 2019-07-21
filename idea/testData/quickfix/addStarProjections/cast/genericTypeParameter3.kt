// "Change type arguments to <*>" "true"
fun <T> test(list: List<*>): List<*> {
    return list as List<T><caret>
}