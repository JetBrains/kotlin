// "Replace with 'this'" "true"
class C {
    deprecated("", ReplaceWith("this"))
    fun oldFun(): C = this
}

fun foo() {
    C().<caret>oldFun()
}
