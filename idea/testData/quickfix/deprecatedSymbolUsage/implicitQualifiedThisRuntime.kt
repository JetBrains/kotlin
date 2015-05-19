// "Replace with 'newFun(this, s)'" "true"

interface I {
    @deprecated("", ReplaceWith("newFun(this, s)"))
    fun oldFun(s: String)
}

fun newFun(i: I, s: String){}

fun I.foo() {
    with("a") {
        <caret>oldFun(this)
    }
}

