// "Replace with 'newFun(*elements)'" "true"
// WITH_RUNTIME

@Deprecated("", ReplaceWith("newFun(*elements)"))
fun oldFun(vararg elements: java.io.File?) {
    newFun(*elements)
}

fun newFun(vararg elements: java.io.File?){}

fun foo() {
    <caret>oldFun()
}
