// "Make Data public" "true"

private data class Data(val x: Int)

class First {
    val <caret>foo = Data(13)
}
