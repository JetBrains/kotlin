// "Replace with 'newFun(this, p)'" "true"

class C {
    @Deprecated("", ReplaceWith("newFun(this, p)"))
    fun oldFun(p: Int){}
}

fun C.newFun(c: C, p: Int){}

val bar = 0

fun foo() {
    getBar().<caret>oldFun(bar)
}

fun getBar(): C = C()
