// "Replace with 'newFun(c)'" "true"

@deprecated("", ReplaceWith("newFun(c)"))
fun oldFun(c: Char){}

fun newFun(c: Char){}

fun foo() {
    <caret>oldFun(java.io.File.separatorChar)
}
