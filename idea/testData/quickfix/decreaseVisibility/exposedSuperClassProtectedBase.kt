// "Make First private" "true"

private open class Data(val x: Int)

class Outer {
    protected class First : <caret>Data(42)
}