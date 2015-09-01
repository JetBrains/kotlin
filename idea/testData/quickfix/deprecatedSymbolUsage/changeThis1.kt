// "Replace with 's.newFun(this)'" "true"

class X {
    @Deprecated("", ReplaceWith("s.newFun(this)"))
    fun oldFun(s: String){}
}

fun String.newFun(x: X){}

fun foo(x: X) {
    x.<caret>oldFun("a")
}
