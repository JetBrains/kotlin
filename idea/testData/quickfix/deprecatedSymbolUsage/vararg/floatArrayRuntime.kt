// "Replace with 'newFun(p)'" "true"
// WITH_RUNTIME

@Deprecated("", ReplaceWith("newFun(p)"))
fun oldFun(vararg p: Float){
    newFun(p)
}

fun newFun(p: FloatArray){}

fun foo() {
    <caret>oldFun(1f)
}
