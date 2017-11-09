data class DataIntString(var i: Int, var s: String)
fun destruct(i: Int, s: String, lambda: (DataIntString) -> Unit) = lambda(DataIntString(i, s))
fun useDestruct() {
    destruct(0, "a") { (x, y): DataIntString<caret> -> }
}