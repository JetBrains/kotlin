// "Insert 'super()' call" "false"
// ACTION: Insert 'this()' call
// ERROR: Primary constructor call expected

open class B()

class A(val x: Int) : B() {
    constructor(x: String)<caret>
}
