// PROBLEM: none
open class A1 {
    open var a: String = ""
}

class A2 : A1() {
    override lateinit var <caret>a: String
}