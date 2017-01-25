data class DataIntString(var i: Int, var s: String)
fun destruct(i: Int, s: String, lambda: (DataIntString, Int) -> Unit) = lambda(DataIntString(i, s), i)
fun useDestruct() {
    destruct(0, "a") { (x, y): DataIntString, i: Int<caret> -> }
}