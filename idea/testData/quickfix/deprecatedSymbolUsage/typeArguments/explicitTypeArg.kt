// "Replace with 'newFun<T>()'" "true"

@deprecated("", ReplaceWith("newFun<T>()"))
fun <T> oldFun() {
    newFun<T>()
}

fun <T> newFun(){}

fun foo() {
    <caret>oldFun<String>()
}
