// "Make 'foo' private" "true"
// ACTION: Make 'Data' public

private data class Data(val x: Int)

class First {
    val <caret>foo = Data(13)
}
