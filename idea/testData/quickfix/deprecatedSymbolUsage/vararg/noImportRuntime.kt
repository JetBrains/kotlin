// "Replace with 'newFun(*elements)'" "true"

@Deprecated("", ReplaceWith("newFun(*elements)"))
fun oldFun(vararg elements: java.io.File?) {
    newFun(*elements)
}

fun newFun(vararg elements: java.io.File?){}

fun foo() {
    <caret>oldFun()
}
