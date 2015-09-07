// "Replace with 'newFun(p1, *p2)'" "true"

@Deprecated("", ReplaceWith("newFun(p1, *p2)"))
fun oldFun(p1: String, vararg p2: Int){
    newFun(p1, *p2)
}

fun newFun(p1: String, vararg p2: Int){}

fun foo() {
    <caret>oldFun("a", 1)
}
