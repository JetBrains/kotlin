// "Make 'MyClass' open" "false"
// ACTION: Make 'y' not open
// ACTION: Add 'const' modifier
// ACTION: Make internal
// ACTION: Make private
// ACTION: Specify type explicitly

// See KT-11003
class MyClass {
    companion object {
        <caret>open val y = 4
    }
}