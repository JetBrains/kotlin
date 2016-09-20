// "Change return type of current function 'boo' to 'String'" "true"
fun boo(): Int {
    return ((if (true) {
        val a = ""
        <caret>a
    } else ""))
}