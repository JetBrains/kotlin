// "Replace with 'newFun1(newFun2())'" "true"

class X {
    @Deprecated("", ReplaceWith("newFun1(newFun2())"))
    fun oldFun() {
        newFun1(newFun2())
    }

    fun newFun1(p: Int): Int = p
    fun newFun2(): Int = 1
}

fun foo(x: X) {
    x.<caret>oldFun()
}
