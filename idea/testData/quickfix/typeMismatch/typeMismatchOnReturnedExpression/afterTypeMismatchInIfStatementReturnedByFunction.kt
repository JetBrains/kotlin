// "Change 'boo' function return type to 'String'" "true"
fun boo(): String {
    return ((if (true) {
        val a = ""
        <caret>a
    } else ""))
}