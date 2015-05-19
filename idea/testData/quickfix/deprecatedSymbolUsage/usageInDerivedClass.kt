// "Replace with 'newFun(s)'" "true"

open class Base {
    @deprecated("", ReplaceWith("newFun(s)"))
    fun oldFun(s: String){}

    fun newFun(s: String){}
}

class Derived : Base() {
    fun foo() {
        <caret>oldFun("a")
    }
}
