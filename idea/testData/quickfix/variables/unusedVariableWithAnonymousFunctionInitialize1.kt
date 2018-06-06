// "Remove variable 'a'" "false"
// ACTION: Split property declaration
fun test() {
    val <caret>a: (String) -> Unit = fun(s) { s + s }
}