// "Add initializer" "false"
// ACTION: Add getter
// ACTION: Make internal
// ACTION: Make private
// ACTION: Make protected
// ERROR: Property must be initialized
class A {
    <caret>var Int.n: Int
        set(value: Int) {}
}