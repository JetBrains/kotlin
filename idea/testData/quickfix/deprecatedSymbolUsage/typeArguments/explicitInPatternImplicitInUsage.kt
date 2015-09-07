// "Replace with 'newFun<T>()'" "true"

@Deprecated("", ReplaceWith("newFun<T>()"))
fun <T : Any> oldFun(): T? {
    return newFun<T>()
}

fun <T : Any> newFun(): T? = null

fun foo(): String? {
    return <caret>oldFun()
}
