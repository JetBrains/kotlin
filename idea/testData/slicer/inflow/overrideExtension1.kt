// FLOW: IN

open class Base {
    open fun Int.extensionFun() {
        val v = <caret>this
    }

    fun baseF() {
        1.extensionFun()
    }
}

class Derived : Base() {
    override fun Int.extensionFun() {
    }

    fun derivedF() {
        2.extensionFun()
    }
}