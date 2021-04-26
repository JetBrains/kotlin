// "Add initializer" "false"
// ACTION: Add setter
// ACTION: Change to val
// ACTION: Make internal
// ACTION: Make private
// ACTION: Make protected
// ACTION: Remove explicit type specification
// ERROR: Property must be initialized
class A {
    <caret>var Int.n: Int
        get() = 1
}