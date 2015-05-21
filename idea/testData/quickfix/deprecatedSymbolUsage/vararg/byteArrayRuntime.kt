// "Replace with 'newFun(p)'" "true"

@deprecated("", ReplaceWith("newFun(p)"))
fun oldFun(vararg p: Byte){
    newFun(p)
}

fun newFun(p: ByteArray){}

fun foo() {
    <caret>oldFun(1, 2, 3)
}
