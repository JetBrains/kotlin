// "Replace with 'newFun(p)'" "true"

@Deprecated("", ReplaceWith("newFun(p)"))
fun oldFun(vararg p: Long){
    newFun(p)
}

fun newFun(p: LongArray){}

fun foo() {
    <caret>oldFun(1L)
}
