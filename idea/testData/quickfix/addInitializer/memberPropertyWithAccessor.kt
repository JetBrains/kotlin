// "Add initializer" "false"
// ACTION: Make internal
// ACTION: Make private
// ACTION: Make protected
// ACTION: Remove explicit type specification
class A {
    <caret>val n: Int
        get() = 1
}