// "Make 'First' private" "true"
// ACTION: Make 'Data' public

private open class Data(val x: Int)

class Outer {
    protected class First : <caret>Data(42)
}