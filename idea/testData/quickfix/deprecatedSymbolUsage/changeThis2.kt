// "Replace with 's.newFun(this)'" "true"

class X {
    @Deprecated("", ReplaceWith("s.newFun(this)"))
    fun oldFun(s: String){}
}

fun String.newFun(x: X){}

fun X.foo() {
    <caret>oldFun("a")
}
