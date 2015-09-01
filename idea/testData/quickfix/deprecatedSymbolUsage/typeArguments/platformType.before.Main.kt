// "Replace with 'newFun()'" "true"

@Deprecated("", ReplaceWith("newFun()"))
fun <T> Collection<T>.oldFun() {}

fun <T> Collection<T>.newFun() {}

fun foo() {
    JavaClass.list().<caret>oldFun()
}