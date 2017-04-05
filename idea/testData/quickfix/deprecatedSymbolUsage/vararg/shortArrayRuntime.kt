// "Replace with 'newFun(p)'" "true"

@Deprecated("", ReplaceWith("newFun(p)"))
fun oldFun(vararg p: Short){
    newFun(p)
}

fun newFun(p: ShortArray){}

fun foo() {
    <caret>oldFun(1)
}
