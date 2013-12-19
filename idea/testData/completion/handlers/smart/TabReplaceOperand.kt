var b1: Boolean = true
var b2: Boolean = true
var b3: Boolean = true
val c: String = ""

fun bar() {
    if (b1 && <caret>b2 && b3) {}
}
