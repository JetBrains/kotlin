// "Remove variable 'a'" "true"
fun test() {
    val <caret>a: (String) -> Unit = fun(s: String) { s + s }
}