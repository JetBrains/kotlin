// "class org.jetbrains.kotlin.idea.quickfix.InitializePropertyQuickFixFactory$MoveToConstructorParameters" "false"
// ACTION: Initialize property 'n'
// ACTION: Make 'n' abstract
// ACTION: Make internal
// ACTION: Make private
// ACTION: Make protected
// ACTION: Initialize with constructor parameter
// ERROR: Property must be initialized or be abstract
open class A(x: Int)

class B : A {
    <caret>val n: Int

    constructor(): super(1)
}