// "Make 'Data' internal" "true"
// ACTION: Make 'bar' private
// ACTION: Make 'Data' public

private data class Data(val x: Int)

class First {
    internal fun <caret>bar(x: Int) = Data(x)
}
