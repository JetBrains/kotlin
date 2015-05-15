// "Replace with 'newFun()'" "true"

class X {
    @deprecated("", ReplaceWith("newFun()"))
    fun oldFun(){}

    fun newFun(){}
}

fun foo(x: X?) {
    x?.<caret>oldFun()
}
