// "Make 'bar' private" "true"
// ACTION: Make 'Data' internal

private data class Data(val x: Int)

class First {
    internal fun <caret>bar(x: Int) = Data(x)
}
