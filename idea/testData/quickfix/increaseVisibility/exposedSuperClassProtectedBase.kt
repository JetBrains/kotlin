// "Make 'Data' public" "true"
// ACTION: Make 'First' private

private open class Data(val x: Int)

class Outer {
    protected class First : <caret>Data(42)
}