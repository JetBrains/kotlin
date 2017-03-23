// "Replace with 'newFun(*c)'" "true"
// WITH_RUNTIME

@Deprecated("", ReplaceWith("newFun(*c)"))
fun oldFun(vararg c: Char){}

fun newFun(vararg c: Char){}

fun foo() {
    <caret>oldFun(*listOf(java.io.File.separatorChar).toCharArray())
}
