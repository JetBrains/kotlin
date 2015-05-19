// "Replace with 'newFun(this)'" "true"

class C {
    @deprecated("", ReplaceWith("newFun(this)"))
    fun oldFun(){}
}

fun C.newFun(c: C){}

fun foo() {
    val bar = 0
    getBar().<caret>oldFun()
}

fun getBar(): C = C()
