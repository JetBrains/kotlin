// "Make bar internal" "false"
// ACTION: Convert property initializer to getter
// ACTION: Add names to call arguments
// ACTION: Move to constructor
// ACTION: Convert to run
// ACTION: Convert to with
// ERROR: Cannot access 'bar': it is private in 'First'

private data class Data(val x: Int)

class First {
    // Making it internal exposes 'Data'
    private fun bar(x: Int) = Data(x)
}

class Second(f: First) {
    private val y = f.<caret>bar(42)
}
