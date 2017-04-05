// "Replace with 'newFun(*p, x = null)'" "true"

@Deprecated("", ReplaceWith("newFun(*p, x = null)"))
fun oldFun(vararg p: Int){
    newFun(*p, x = null)
}

fun newFun(vararg p: Int, x: String? = ""){}

fun foo() {
    <caret>oldFun(1, 2, 3)
}
