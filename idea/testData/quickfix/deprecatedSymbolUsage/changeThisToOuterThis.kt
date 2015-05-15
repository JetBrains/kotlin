// "Replace with 's.newFun(this)'" "true"

open class Base {
    @deprecated("", ReplaceWith("s.newFun(this)"))
    fun oldFun(s: String){}

    open inner class Inner
}

class Derived : Base() {
    inner class InnerDerived : Base.Inner() {
        fun foo() {
            <caret>oldFun("a")
        }
    }
}

fun String.newFun(x: Base){}
