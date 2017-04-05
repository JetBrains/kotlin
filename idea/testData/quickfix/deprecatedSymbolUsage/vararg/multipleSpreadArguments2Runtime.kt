// "Replace with 'newFun(p)'" "true"

@Deprecated("", ReplaceWith("newFun(p)"))
fun oldFun(vararg p: Int){
    newFun(p)
}

fun newFun(p: IntArray){}

fun foo(list1: List<Int>,list2: List<Int>) {
    <caret>oldFun(*list1.toIntArray(), 0, *list2.toIntArray())
}
