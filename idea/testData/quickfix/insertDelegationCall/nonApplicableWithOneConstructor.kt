// "class org.jetbrains.kotlin.idea.quickfix.InsertDelegationCallQuickfix" "false"
// ACTION: Insert 'super()' call
// ACTION: Convert to primary constructor
// ERROR: Explicit 'this' or 'super' call is required. There is no constructor in superclass that can be called without arguments

open class B(val x: Int)

class A : B {
    constructor(x: String)<caret>
}
