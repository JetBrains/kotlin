// "Replace with 'newFun(*p, *list.toIntArray())'" "true"

@Deprecated("", ReplaceWith("newFun(*p, *list.toIntArray())"))
fun oldFun(list: List<Int>, vararg p: Int){
    newFun(*p, *list.toIntArray())
}

fun newFun(vararg p: Int){}

fun foo(list: List<Int>) {
    <caret>oldFun(list, 1, 2, 3)
}
