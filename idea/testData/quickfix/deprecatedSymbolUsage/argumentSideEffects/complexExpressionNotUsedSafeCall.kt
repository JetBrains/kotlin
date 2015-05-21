// "Replace with 'newFun()'" "true"

class C {
    @deprecated("", ReplaceWith("newFun()"))
    fun oldFun() {
        newFun()
    }
}

fun newFun(){}

fun foo() {
    getC()?.<caret>oldFun()
}

fun getC(): C? = null
