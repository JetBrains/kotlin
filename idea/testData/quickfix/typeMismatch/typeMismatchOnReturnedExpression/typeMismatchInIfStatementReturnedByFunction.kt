// "Change 'boo' function return type to 'String'" "true"
fun boo(): Int {
    return ((if (true) {
        val a = ""
        <caret>a
    } else ""))
}