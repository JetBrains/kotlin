// "Make Data protected" "true"

class Outer {
    private open class Data(val x: Int)

    protected class First : <caret>Data(42)
}