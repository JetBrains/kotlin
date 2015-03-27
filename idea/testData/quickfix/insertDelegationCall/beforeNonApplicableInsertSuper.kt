// "Insert 'super()' call" "true"
// ERROR: No value passed for parameter x

open class B(val x: Int)

class A : B {
    constructor(x: String)<caret>
}
